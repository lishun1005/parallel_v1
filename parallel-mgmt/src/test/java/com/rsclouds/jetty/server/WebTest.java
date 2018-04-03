package com.rsclouds.jetty.server;

import com.rscloud.ipc.contrller.AiModelContrlloer;
import com.rscloud.ipc.rpc.api.service.AiModelService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/2/2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("dev")
@ContextConfiguration(
		locations={ "classpath:spring-mvc.xml",
					"classpath:app.xml",  "classpath:app-dubbo.xml"})

@WebAppConfiguration
public class WebTest {
	@Autowired
	private WebApplicationContext wac;

	@InjectMocks
	private AiModelContrlloer aiModelContrlloer;

	@Mock
	private AiModelService aiModelService;

	private MockMvc mockMvc;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.mockMvc = MockMvcBuilders.standaloneSetup(aiModelContrlloer).build();
	}
	@Test
	public void testAddConsultContract() throws Exception{
		//resultAction是用来模拟客户端请求
		ResultActions resultActions =
				this.mockMvc.perform(
						MockMvcRequestBuilders.post("/ai/model/add")
						.accept(MediaType.APPLICATION_JSON)
								.param("name","模型1qqq2")
								.param("aiModelParams[4].id","12121")
								.param("aiModelParams[0].id","12121")
								.param("aiModelParams[1].id","12121")
								.param("aiModelParams[2].id","12121")
						);

		//MvcResult是获得服务器的Response内容。
		MvcResult mvcResult = resultActions.andReturn();
		String result = mvcResult.getResponse().getContentAsString();
		System.out.println("*******:" + result);

	}
}
