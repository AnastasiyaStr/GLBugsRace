package com.basecamp.service.impl;

import com.basecamp.exception.InternalException;
import com.basecamp.exception.InvalidDataException;
import com.basecamp.service.ProductService;
import com.basecamp.wire.GetHandleProductIdsResponse;
import com.basecamp.wire.GetProductInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ProductServiceImpl implements ProductService {

    private final ConcurrentTaskService taskService;

    public GetProductInfoResponse getProductInfo(String productId) {

        validateId(productId);

        log.info("Product id {} was successfully validated.", productId);

        return callToDbAnotherServiceETC(productId);
    }

    public GetHandleProductIdsResponse handleProducts(List<String> productIds) {
        Map<String, Future<String>> handledTasks = new HashMap<>();
        productIds.forEach(productId ->
                handledTasks.put(
                        productId,
                        taskService.handleProductIdByExecutor(productId)));

        List<String> handledIds = handledTasks.entrySet().stream().map(stringFutureEntry -> {
            try {
                return stringFutureEntry.getValue().get(3, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error(stringFutureEntry.getKey() + " execution error!");
            }

            return stringFutureEntry.getKey() + " is not handled!";
        }).collect(Collectors.toList());

        return GetHandleProductIdsResponse.builder()
                .productIds(handledIds)
                .build();
    }

    public void stopProductExecutor() {
        log.warn("Calling to stop product executor...");

        taskService.stopExecutorService();

        log.info("Product executor stopped.");
    }

    private void validateId(String id) {

        if (StringUtils.isEmpty(id)) {
            // all messages could be moved to messages properties file (resources)
            String msg = "ProductId is not set.";
            log.error(msg);
            throw new InvalidDataException(msg);
        }

        try {
            Integer.valueOf(id);
        } catch (NumberFormatException e) {
            String msg = String.format("ProductId %s is not a number.", id);
            log.error(msg);
            throw new InvalidDataException(msg);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new InternalException(e.getMessage());
        }
    }

    private GetProductInfoResponse callToDbAnotherServiceETC(String productId) {
        return GetProductInfoResponse.builder()
                .id(productId)
                .name("ProductName")
                .status("ProductStatus")
                .build();
    }

    @Override
    public void bugsRace() {
        Map<String, BlockingQueue<Integer>> map =new ConcurrentHashMap<>(5);
        ExecutorService service = Executors.newFixedThreadPool(3);
        Callable<String> task = () -> {
            int bug = 0;
            String threadKey=null;
            while (bug < 100) {
                Random random = new Random();
                bug += random.nextInt(50);
                String name = Thread.currentThread().getName();
                int threadNumber = name.length()-1;
                threadKey= name.substring(threadNumber);
                if(!map.containsKey(threadKey)){
                    BlockingQueue<Integer> list = new ArrayBlockingQueue<>(50);
                    list.add(bug);
                    map.put(threadKey,list);
                }
                else {
                    map.get(threadKey).add(bug);
                }
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            map.get(threadKey).add(bug);

            return Thread.currentThread().getName();
        };
        List<Callable<String>> callableTasks = new ArrayList<>();

        callableTasks.add(task);
        callableTasks.add(task);
        callableTasks.add(task);


        try {
            service.invokeAll(callableTasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        int l=1;
        System.out.println("\t\t\t\t\t\t**********Bugs Race**************");
        while(true) {
            int j;
            log.info("LOOP "+l);
            for ( j = 1; j <= 3; j++) {
                String s = String.valueOf(j);
                int p = 0;
                try {
                    p = map.get(s).take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (int k = 0;  k < p; k++) {
                    System.out.print("-");
                }
                System.out.println(s);

                if(p>=100){
                    System.out.println(s+ " WON!!!");
                    break;
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(j<=3)
                break;
            l++;

        }
        service.shutdownNow();
    }

}
