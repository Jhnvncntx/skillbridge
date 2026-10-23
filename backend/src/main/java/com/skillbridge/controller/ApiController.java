package com.skillbridge.controller;

import com.skillbridge.dto.Requests.*;
import com.skillbridge.dto.Responses.*;
import com.skillbridge.service.ApiException;
import com.skillbridge.service.SkillBridgeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Thin REST layer: every endpoint just calls the service, which uses the custom data structures. */
@RestController
@RequestMapping("/api")
public class ApiController {
    private final SkillBridgeService service;
    private final String adminPasscode = System.getenv().getOrDefault("ADMIN_PASSCODE", "admin123");

    public ApiController(SkillBridgeService service) { this.service = service; }

    @GetMapping("/health")
    public HealthDto health() { return service.health(); }

    @GetMapping("/programs")
    public ProgramDto[] programs() { return service.programs(); }

    // ---- users
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(@RequestBody RegisterRequest req) { return service.registerUser(req); }

    @GetMapping("/users/{id}")
    public UserDto getUser(@PathVariable("id") int id) { return service.getUser(id); }

    @PutMapping("/users/{id}/skills")
    public UserDto setSkills(@PathVariable("id") int id, @RequestBody SkillsRequest req) {
        return service.setSkills(id, req);
    }

    // ---- skills
    @GetMapping("/skills")
    public String[] skills() { return service.skillIndex(); }

    @GetMapping("/skills/check")
    public SkillCheckDto checkSkill(@RequestParam(value = "name", defaultValue = "") String name) {
        return service.checkSkill(name);
    }

    // ---- jobs
    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public JobDto postJob(@RequestBody JobRequest req) { return service.postJob(req); }

    @GetMapping("/jobs")
    public JobDto[] jobs() { return service.listJobs(); }

    // ---- matching
    @GetMapping("/users/{id}/recommendations")
    public MatchDto[] recommendations(@PathVariable("id") int id,
                                      @RequestParam(value = "top", defaultValue = "5") int top) {
        return service.recommendations(id, top);
    }

    @GetMapping("/users/{id}/ranked")
    public MatchDto[] ranked(@PathVariable("id") int id) { return service.ranked(id); }

    // ---- applications
    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDto apply(@RequestBody ApplyRequest req) { return service.apply(req); }

    @GetMapping("/users/{id}/applications")
    public ApplicationDto[] applications(@PathVariable("id") int id) { return service.userApplications(id); }

    @DeleteMapping("/users/{id}/applications/last")
    public ApplicationDto undoLast(@PathVariable("id") int id) { return service.undoLastApplication(id); }

    // ---- admin (simple passcode in a header; no real login, as stated in the paper's limitations)
    @GetMapping("/admin/stats")
    public StatsDto stats(@RequestHeader(value = "X-Admin-Passcode", required = false) String passcode) {
        if (passcode == null || !passcode.equals(adminPasscode))
            throw ApiException.unauthorized("Invalid or missing admin passcode.");
        return service.stats();
    }
}
