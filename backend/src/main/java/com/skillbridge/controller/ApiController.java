package com.skillbridge.controller;

import com.skillbridge.dto.Requests.*;
import com.skillbridge.dto.Responses.*;
import com.skillbridge.service.ApiException;
import com.skillbridge.service.SkillBridgeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Thin REST layer: every endpoint calls the service, which uses our own data structures.
 * Logged-in endpoints read the session token from the "Authorization: Bearer <token>" header.
 */
@RestController
@RequestMapping("/api")
public class ApiController {
    private final SkillBridgeService service;
    private final String adminPasscode = System.getenv().getOrDefault("ADMIN_PASSCODE", "admin123");

    public ApiController(SkillBridgeService service) { this.service = service; }

    // ---- public
    @GetMapping("/health")
    public HealthDto health() { return service.health(); }

    @GetMapping("/programs")
    public ProgramDto[] programs() { return service.programList(); }

    @GetMapping("/programs/tree")
    public TreeDto programTree() { return service.programTree(); }

    @GetMapping("/programs/{code}/roles")
    public String[] roles(@PathVariable("code") String code) { return service.rolesFor(code); }

    @GetMapping("/programs/{code}/skills")
    public String[] programSkills(@PathVariable("code") String code) { return service.skillsFor(code); }

    @GetMapping("/skills")
    public String[] skills(@RequestParam(value = "prefix", defaultValue = "") String prefix) {
        return service.skillSuggestions(prefix);
    }

    // ---- accounts
    @PostMapping("/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDto signup(@RequestBody SignupRequest req) { return service.signup(req); }

    @PostMapping("/auth/login")
    public AuthDto login(@RequestBody LoginRequest req) { return service.login(req); }

    @PostMapping("/auth/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String auth) { service.logout(auth); }

    @GetMapping("/me")
    public UserDto me(@RequestHeader(value = "Authorization", required = false) String auth) {
        return service.me(service.userIdFromToken(auth));
    }

    @PutMapping("/me")
    public UserDto updateMe(@RequestHeader(value = "Authorization", required = false) String auth,
                            @RequestBody ProfileRequest req) {
        return service.updateProfile(service.userIdFromToken(auth), req);
    }

    /** Permanently deletes the logged-in user's account and data (the password is asked again). */
    @PostMapping("/me/delete")
    public void deleteMe(@RequestHeader(value = "Authorization", required = false) String auth,
                         @RequestBody DeleteAccountRequest req) {
        service.deleteAccount(service.userIdFromToken(auth), req);
    }

    // ---- home feed and jobs
    @GetMapping("/me/home")
    public HomeDto home(@RequestHeader(value = "Authorization", required = false) String auth,
                        @RequestParam(value = "top", defaultValue = "6") int top) {
        return service.home(service.userIdFromToken(auth), top);
    }

    @GetMapping("/jobs")
    public MatchDto[] jobs(@RequestHeader(value = "Authorization", required = false) String auth,
                           @RequestParam(value = "q", defaultValue = "") String q) {
        return service.jobs(service.userIdFromToken(auth), q);
    }

    @GetMapping("/jobs/{id}")
    public MatchDto job(@RequestHeader(value = "Authorization", required = false) String auth,
                        @PathVariable("id") String id) {
        return service.jobDetail(service.userIdFromToken(auth), id);
    }

    // ---- saved jobs
    @GetMapping("/me/saved")
    public JobDto[] saved(@RequestHeader(value = "Authorization", required = false) String auth) {
        return service.savedJobs(service.userIdFromToken(auth));
    }

    @PostMapping("/me/saved/{jobId}")
    public JobDto save(@RequestHeader(value = "Authorization", required = false) String auth,
                       @PathVariable("jobId") String jobId) {
        return service.saveJob(service.userIdFromToken(auth), jobId);
    }

    @DeleteMapping("/me/saved/{jobId}")
    public void unsave(@RequestHeader(value = "Authorization", required = false) String auth,
                       @PathVariable("jobId") String jobId) {
        service.unsaveJob(service.userIdFromToken(auth), jobId);
    }

    // ---- applications
    @PostMapping("/me/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDto apply(@RequestHeader(value = "Authorization", required = false) String auth,
                                @RequestBody ApplyRequest req) {
        return service.apply(service.userIdFromToken(auth), req);
    }

    @GetMapping("/me/applications")
    public ApplicationDto[] applications(@RequestHeader(value = "Authorization", required = false) String auth) {
        return service.applications(service.userIdFromToken(auth));
    }

    @DeleteMapping("/me/applications/last")
    public ApplicationDto undoLast(@RequestHeader(value = "Authorization", required = false) String auth) {
        return service.undoLastApplication(service.userIdFromToken(auth));
    }

    // ---- admin (simple passcode in a header; no admin accounts)
    @GetMapping("/admin/stats")
    public StatsDto stats(@RequestHeader(value = "X-Admin-Passcode", required = false) String passcode) {
        if (passcode == null || !passcode.equals(adminPasscode))
            throw ApiException.unauthorized("Invalid or missing admin passcode.");
        return service.stats();
    }
}
