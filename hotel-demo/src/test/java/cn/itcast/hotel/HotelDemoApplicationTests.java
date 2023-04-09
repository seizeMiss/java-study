package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest
class HotelDemoApplicationTests {

    private RestHighLevelClient client;

    @Autowired
    private IHotelService hotelService;

    @Test
    public void testCreateHotelIndex() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest("hotel");

        request.source("", XContentType.JSON);

        client.indices().create(request, RequestOptions.DEFAULT);
    }

    @Test
    public void testDeleteHotelIndex() throws IOException {
        DeleteIndexRequest request = new DeleteIndexRequest("hotel");

        client.indices().delete(request, RequestOptions.DEFAULT);
    }

    @Test
    public void testExistHotelIndex() throws IOException {
        GetIndexRequest request = new GetIndexRequest("hotel");

        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);

        System.out.println(exists);
    }

    @Test
    public void testBulkHotel() throws IOException {
        List<Hotel> hotels = hotelService.list();

        BulkRequest request = new BulkRequest();

        for (Hotel hotel : hotels) {
            HotelDoc hotelDoc = new HotelDoc(hotel);

            request.add(
                    new IndexRequest("hotel")
                            .id(hotel.getId().toString())
                            .source(JSON.toJSONString(hotelDoc),
                                    XContentType.JSON));
        }

        client.bulk(request, RequestOptions.DEFAULT);
    }

    @BeforeEach
    public void client() {
        this.client = new RestHighLevelClient(RestClient.builder(HttpHost.create("http://124.71.152.110:9200")));
    }

    @AfterEach
    public void closeClient() throws IOException {
        this.client.close();
    }

}
