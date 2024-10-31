package com.avatarai.server;

public record ServiceResponse(int statusCode,
                              String response) {
}
