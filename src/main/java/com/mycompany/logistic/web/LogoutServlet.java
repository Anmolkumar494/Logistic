package com.mycompany.logistic.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/logout")
public class LogoutServlet extends JsonServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        json(response, 200, Map.of("ok", true));
    }
}
