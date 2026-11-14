package com.expensetracker.common.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoint under /api so CORS rules have something to apply to. */
@RestController
class CorsTestController {

    @GetMapping("/api/test/cors-probe")
    String probe() {
        return "ok";
    }
}
