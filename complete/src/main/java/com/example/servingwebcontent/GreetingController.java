package com.example.servingwebcontent;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Value;

@Controller
public class GreetingController {
	@Value("${spring.mysecret}")
	private String secretValue;	

	@GetMapping("/greeting")
	public String greeting(@RequestParam(name="name", required=false, defaultValue="World again") String name, Model model) {
		model.addAttribute("name", name + ". See my secret value which is not very secret: " + secretValue);
		return "greeting";
	}

}
