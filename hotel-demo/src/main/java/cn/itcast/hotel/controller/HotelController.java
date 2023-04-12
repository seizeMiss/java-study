package cn.itcast.hotel.controller;

import cn.itcast.hotel.params.PageResult;
import cn.itcast.hotel.params.SearchParams;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("hotel")
public class HotelController {

    @Resource
    private IHotelService hotelService;

    @PostMapping("list")
    public PageResult list(@RequestBody SearchParams searchParams) throws IOException {
        return hotelService.search(searchParams);
    }

    @PostMapping("filters")
    public Map<String, List<String>> getFilters(@RequestBody SearchParams searchParams) {
        return hotelService.getFilters(searchParams);
    }

    @GetMapping("suggestion")
    public List<String> getSuggestion(@RequestParam("key") String key) {
        return hotelService.getSuggestion(key);
    }

}
