package com.isaacsek.SpringRestApi.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.isaacsek.SpringRestApi.interfaces.IShotDetectionService;

@Controller
public class ShotDetectionController {
	
	@Value("${foo.email}")
	private String email;
	
	@Autowired
	private IShotDetectionService _service;
	
	public ShotDetectionController(IShotDetectionService service) {
		_service = service;
	}
	
	@RequestMapping("/test")
	@ResponseBody
    public int Test(@RequestParam(value="name", defaultValue="World") String name) {
    	return _service.getBoundaries();
    }
	
	@RequestMapping("/email")
	@ResponseBody
    public String Email(@RequestParam(value="name", defaultValue="World") String name) {
    	return email;
    }
	
	@GetMapping("/greeting")
    public String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
        model.addAttribute("name", name);
        return "greeting";
    }
    
//    @RequestMapping("/")
//    @ResponseBody
//    public String Welcome() {
//    	return "index";
//    }
}
