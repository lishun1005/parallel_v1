package com.rsclouds.ai;

import com.rscloud.ipc.rpc.api.service.AiModelService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/2/2
 */
//@RunWith(SpringRunner.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@DirtiesContext
public class SpringTest {
	@Autowired
	private AiModelService aiModelService;

	@Test
	public void findOne() throws Exception {
		//List<AiModelTest> list = aiModelService.test();
		//System.out.println(list.size());
	}
	public static void  main(String[] args){
		RealClass r = new RealClass();

		new ProxyStaticClass(r).request("lishun");

		ProxyDynimacClass p = new ProxyDynimacClass(r);
		Subject s=(Subject) Proxy
				.newProxyInstance(RealClass.class.getClassLoader(),RealClass.class.getInterfaces(), p);
		//ProxyClient
		s.request("dynamic");
	}
}

interface Subject {
	public void request(String action);
}
class RealClass implements Subject{
	private Integer age;

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	@Override
	public void request(String action) {
		System.out.println("Proxy "+action);
	}
}
class ProxyStaticClass implements Subject{
	private Subject sub;
	public ProxyStaticClass(Subject obj){
		this.sub=obj;
	}
	@Override
	public void request(String action) {
		sub.request(action);;
	}
}
class ProxyDynimacClass implements InvocationHandler {
	private Object sub;
	public ProxyDynimacClass(Object obj){
		this.sub = obj;
	}
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		return method.invoke(sub, args);
	}

}
