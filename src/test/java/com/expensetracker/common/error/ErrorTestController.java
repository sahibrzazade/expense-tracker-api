package com.expensetracker.common.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoints that trigger each error path. Lives in src/test, so it never ships. */
@RestController
@RequestMapping("/test/errors")
class ErrorTestController {

    record CreateItemRequest(@NotBlank String name) {}

    @PostMapping("/validation")
    void validateBody(@Valid @RequestBody CreateItemRequest request) {}

    @GetMapping("/validation")
    void validateParam(@RequestParam @Min(1) int page) {}

    @GetMapping("/not-found")
    void notFound() {
        throw new NotFoundException("Item 42 not found");
    }

    @GetMapping("/conflict")
    void conflict() {
        throw new ConflictException("Item is still in use");
    }

    @GetMapping("/unexpected")
    void unexpected() {
        throw new IllegalStateException("secret internal detail");
    }
}
