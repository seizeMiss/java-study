package cn.itcast.hotel.config;

import cn.itcast.hotel.constants.HotelMqConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class RabbitmqConfig {

    @Bean
    public Exchange hotelExchange() {
        return new TopicExchange(HotelMqConstants.EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue hotelUpdateQueue() {
        return new Queue(HotelMqConstants.INSERT_QUEUE_NAME, true);
    }

    @Bean
    public Queue hotelDeleteQueue() {
        return new Queue(HotelMqConstants.DELETE_QUEUE_NAME, true);
    }

    @Bean
    public Binding hotelUpdateBinding(Exchange hotelExchange, Queue hotelUpdateQueue) {
        return BindingBuilder.bind(hotelUpdateQueue).to(hotelExchange).with(HotelMqConstants.INSERT_KEY).noargs();
    }

    @Bean
    public Binding hotelDeleteBinding(Exchange hotelExchange, Queue hotelDeleteQueue) {
        return BindingBuilder.bind(hotelDeleteQueue).to(hotelExchange).with(HotelMqConstants.DELETE_KEY).noargs();
    }
}
