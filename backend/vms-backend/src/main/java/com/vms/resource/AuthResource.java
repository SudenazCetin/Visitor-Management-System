package com.vms.resource;

import com.vms.dto.auth.LoginRequest;
import com.vms.dto.auth.LoginResponse;
import com.vms.dto.auth.RegisterRequest;
import com.vms.service.AuthService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
public class AuthResource {

    private final AuthService authService;

    @Inject
    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/register")
    @PermitAll
    public Response register(@Valid RegisterRequest request) {
        authService.register(request);
        return Response.status(Response.Status.CREATED)
                .entity(Map.of("message", "Kayıt başarılı."))
                .build();
    }
}
