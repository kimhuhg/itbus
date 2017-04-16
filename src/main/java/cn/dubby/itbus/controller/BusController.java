package cn.dubby.itbus.controller;

import cn.dubby.itbus.bean.Bus;
import cn.dubby.itbus.service.BusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by yangzheng03 on 2017/4/16.
 */
@RestController
@RequestMapping(value = "bus")
public class BusController {

    private final Logger logger = LoggerFactory.getLogger(BusController.class);

    private static final int TOP_NUM = 20;

    @Autowired
    private BusService busService;

    @RequestMapping(value = "index")
    public Object indexList() {
        List<Bus> busList = null;
        try {
            busList = busService.selectTopN(TOP_NUM);
        } catch (Exception e) {
            logger.error("indexList error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(busList);
    }

    @RequestMapping(value = "detail")
    public Object detail(int id) {
        Bus bus = null;
        try {
            bus = busService.detail(id);
        } catch (Exception e) {
            logger.error("detail error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(bus);
    }

}
