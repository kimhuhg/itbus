package cn.dubby.itbus.service;

import cn.dubby.itbus.bean.Bus;
import cn.dubby.itbus.dao.BusDao;
import cn.dubby.itbus.mapper.EmailMapper;
import cn.dubby.itbus.service.dto.CountDto;
import cn.dubby.itbus.service.dto.ModifyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Created by yangzheng03 on 2017/4/15.
 */
@Service
public class BusService {

    private static final Logger logger = LoggerFactory.getLogger(BusService.class);

    @Autowired
    private BusDao busDao;

    @Autowired
    private EmailMapper emailMapper;

    @Autowired
    private EmailService emailService;

    private static final int MAX_NUM = 100;
    private static final int MIN_NUM = 0;

    private static final int PAGE_SIZE = 20;

    public List<Bus> selectTopN(int limit) {
        if (limit < MIN_NUM || limit > MAX_NUM) {
            limit = MAX_NUM;
        }
        return busDao.selectTopN(limit);
    }

    public List<Bus> listByLine(int lineId, int pageId) {
        if (pageId <= 1) {
            pageId = 1;
        }
        int offset = (pageId - 1) * PAGE_SIZE;
        return busDao.selectByLine(lineId, offset, PAGE_SIZE);
    }

    public List<Bus> mine(int userId) {
        return busDao.selectByUserId(userId);
    }

    public CountDto countByLine(int lineId, int pageId) {
        if (pageId <= 1) {
            pageId = 1;
        }
        int count = busDao.countByLine(lineId);
        int pageCount = count / PAGE_SIZE;
        if (count % PAGE_SIZE > 0) {
            ++pageCount;
        }
        if (pageId > pageCount) {
            pageId = pageCount;
        }
        List<Integer> pageIdList = generateCountList(pageId, pageCount);

        return new CountDto(pageId, pageIdList);
    }

    private List<Integer> generateCountList(int currentPageId, int totalCount) {
        LinkedList<Integer> pageIdList = new LinkedList<>();
        pageIdList.add(currentPageId);
        for (int i = 1; i <= 7; ++i) {
            int tempPageId = currentPageId - i;
            if (tempPageId >= 1) {
                pageIdList.offerFirst(tempPageId);
                if (pageIdList.size() >= 7) {
                    return pageIdList;
                }
            }
            tempPageId = currentPageId + i;
            if (tempPageId <= totalCount) {
                pageIdList.offerLast(tempPageId);
                if (pageIdList.size() >= 7) {
                    return pageIdList;
                }
            }
        }

        return pageIdList;
    }

    public Bus detail(int busId) {
        Bus bus = busDao.selectByPrimaryKey(busId);
        if (bus == null) {
            logger.error("detail not exist,id:" + busId);
            return null;
        }

        bus.setBusTicket(null);
        return bus;
    }

    public Bus next(int busId) {
        Bus currentBus = busDao.selectByPrimaryKey(busId);

        Bus nextBus = busDao.selectNext(currentBus.getBusLineId(), currentBus.getId());

        if (nextBus == null) {
            logger.error("detail not exist,id:" + busId);
            return null;
        }

        nextBus.setBusTicket(null);
        return nextBus;
    }

    public Bus prev(int busId) {
        Bus currentBus = busDao.selectByPrimaryKey(busId);

        Bus prevBus = busDao.selectPrev(currentBus.getBusLineId(), currentBus.getId());

        if (prevBus == null) {
            logger.error("detail not exist,id:" + busId);
            return null;
        }

        prevBus.setBusTicket(null);
        return prevBus;
    }

    public ModifyResult<Bus> save(int busLineId, String busName, String busContent, String email) {
        if (busLineId < 0 || StringUtils.isEmpty(busContent) || StringUtils.isEmpty(busName)) {
            return ModifyResult.PARAMS_ERROR;
        }

        Bus bus = new Bus();
        bus.setBusContent(busContent);
        bus.setBusName(busName);
        bus.setBusLineId(busLineId);
        bus.setBusTicket(UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString());

        try {
            busDao.insertSelective(bus);
            bus = busDao.selectByPrimaryKey(bus.getId());

            if (!StringUtils.isEmpty(email)) {
                emailService.sendWriteThanksEmail(email, bus.getId(), bus.getBusTicket());
            }

        } catch (Exception e) {
            logger.error("save", e);
            return ModifyResult.SYSTEM_EXCEPTION;
        }

        return new ModifyResult<>(bus);
    }

    public ModifyResult<Bus> save(int busLineId, String busName, String busContent, String email, int userId) {
        if (busLineId < 0 || StringUtils.isEmpty(busContent) || StringUtils.isEmpty(busName)) {
            return ModifyResult.PARAMS_ERROR;
        }

        Bus bus = new Bus();
        bus.setBusContent(busContent);
        bus.setBusName(busName);
        bus.setBusLineId(busLineId);
        bus.setBusTicket(UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString());
        bus.setAuthorId(userId);

        try {
            busDao.insertSelective(bus);
            bus = busDao.selectByPrimaryKey(bus.getId());

            if (!StringUtils.isEmpty(email)) {
                emailService.sendWriteThanksEmail(email, bus.getId(), bus.getBusTicket());
            }

        } catch (Exception e) {
            logger.error("save", e);
            return ModifyResult.SYSTEM_EXCEPTION;
        }

        return new ModifyResult<>(bus);
    }

    public ModifyResult<Bus> update(int busId, String ticket, int busLineId, String busName, String busContent) {
        if (busId < 0 || StringUtils.isEmpty(ticket) || busLineId < 0 || StringUtils.isEmpty(busContent) || StringUtils.isEmpty(busName)) {
            return ModifyResult.PARAMS_ERROR;
        }

        Bus bus = busDao.selectByPrimaryKey(busId);
        if (bus == null) {
            logger.error("update not exist,id:" + busId);
            return save(busLineId, busName, busContent, null);
        }

        if (!ticket.equals(bus.getBusTicket())) {
            logger.error("ticket error,id:" + busId + ",ticket:" + ticket);
            return ModifyResult.TICKET_ERROR;
        }

        bus.setCreateTime(null);
        bus.setUpdateTime(null);
        bus.setBusContent(busContent);
        bus.setBusName(busName);
        bus.setBusLineId(busLineId);

        try {
            busDao.updateByPrimaryKeySelective(bus);

            bus = busDao.selectByPrimaryKey(busId);
        } catch (Exception e) {
            logger.error("save", e);
            return ModifyResult.SYSTEM_EXCEPTION;
        }

        return new ModifyResult<>(bus);
    }

    public ModifyResult<Bus> modify(int busId, int busLineId, String busName, String busContent) {
        if (busId < 0 || busLineId < 0 || StringUtils.isEmpty(busContent) || StringUtils.isEmpty(busName)) {
            return ModifyResult.PARAMS_ERROR;
        }

        Bus bus = busDao.selectByPrimaryKey(busId);
        if (bus == null) {
            logger.error("update not exist,id:" + busId);
            return save(busLineId, busName, busContent, null);
        }

        bus.setCreateTime(null);
        bus.setUpdateTime(null);
        bus.setBusContent(busContent);
        bus.setBusName(busName);
        bus.setBusLineId(busLineId);

        try {
            busDao.updateByPrimaryKeySelective(bus);

            bus = busDao.selectByPrimaryKey(busId);
        } catch (Exception e) {
            logger.error("save", e);
            return ModifyResult.SYSTEM_EXCEPTION;
        }

        return new ModifyResult<>(bus);
    }

    public void up(int busId) {
        busDao.up(busId);
    }

    public void down(int busId) {
        busDao.down(busId);
    }
}
