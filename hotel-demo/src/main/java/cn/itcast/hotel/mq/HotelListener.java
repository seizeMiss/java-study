package cn.itcast.hotel.mq;

import cn.itcast.hotel.constants.HotelMqConstants;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class HotelListener {
    @Resource
    private IHotelService hotelService;

    @RabbitListener(queues = HotelMqConstants.INSERT_QUEUE_NAME)
    public void listenHotelUpdateOrInsert(Long id) {
        hotelService.insertById(id);
    }

    @RabbitListener(queues = HotelMqConstants.DELETE_QUEUE_NAME)
    public void listenHotelDelete(Long id) {
        hotelService.deleteById(id);
    }
}
