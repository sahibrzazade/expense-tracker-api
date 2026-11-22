package com.expensetracker.identity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MeController {

    private final CurrentUser currentUser;

    MeController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/api/v1/me")
    UserResponse me() {
        return UserResponse.from(currentUser.get());
    }
}
