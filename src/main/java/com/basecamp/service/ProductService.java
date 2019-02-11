package com.basecamp.service;

import com.basecamp.wire.GetHandleProductIdsResponse;
import com.basecamp.wire.GetProductInfoResponse;

import java.util.List;

public interface ProductService {

    GetProductInfoResponse getProductInfo(String productId);

    GetHandleProductIdsResponse handleProducts(List<String> productIds);

    void stopProductExecutor();

    void bugsRace();

}
