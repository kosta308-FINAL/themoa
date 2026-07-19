package testsupport;

import com.weaone.themoa.common.response.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityTestController {

    @PostMapping("/api/auth/login")
    ApiResponse<String> login() {
        return ApiResponse.success("login");
    }

    @GetMapping("/api/protected")
    ApiResponse<String> protectedApi(Authentication authentication) {
        return ApiResponse.success(String.valueOf(authentication.getPrincipal()));
    }

    @PostMapping("/api/policies/search")
    ApiResponse<String> policySearch() {
        return ApiResponse.success("search");
    }

    @GetMapping("/api/policies/1")
    ApiResponse<String> policyDetail() {
        return ApiResponse.success("detail");
    }

    @GetMapping("/api/policies/1/raw")
    ApiResponse<String> policyRaw() {
        return ApiResponse.success("raw");
    }

    @GetMapping("/api/policies/admin/status")
    ApiResponse<String> policyAdmin() {
        return ApiResponse.success("admin");
    }

    @GetMapping("/api/policies/bookmarks/1")
    ApiResponse<String> policyBookmark() {
        return ApiResponse.success("bookmark");
    }
}
