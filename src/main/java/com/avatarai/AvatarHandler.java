package com.avatarai;

import com.avatarai.server.ServiceHandler;
import com.avatarai.server.ServiceRequest;
import com.avatarai.server.ServiceResponse;

public class AvatarHandler extends ServiceHandler {

    public AvatarHandler()
    {
        addMethod("create", this::doSomething, "field1, field2, field3", "param1, param2");
        addMethod("delete", this::doSomething, "field1, field2, field3", "param1, param2");
        addMethod("present", this::doSomething, "field1, field2, field3", "param1, param2");
        addMethod("train", this::doSomething, "field1, field2, field3", "param1, param2");
    }

    private ServiceResponse doSomething(ServiceRequest request)
    {
//        return new ServiceResponse(200, gson.toJson(item));
        return new ServiceResponse(404, "Not found");
    }
}
