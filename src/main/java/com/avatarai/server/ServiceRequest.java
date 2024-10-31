package com.avatarai.server;

import java.util.List;
import java.util.Map;

public record ServiceRequest(String user,
                             String function,
                             Map<String, List<String>> fields,
                             Map<String, List<String>> params) { }
