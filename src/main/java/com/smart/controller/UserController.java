package com.smart.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.razorpay.*;

import com.smart.dao.MyOrderRepository;
import com.smart.entities.MyOrder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private MyOrderRepository myOrderRepository;
	
	public void addCommonData(Model m,Principal principal) {
		
		String userName = principal.getName();
		User user = userRepository.getUserByUserName(userName);
		m.addAttribute("user",user);
	}
	
	@RequestMapping("/index")
	public String dashboard(Model m,Principal principal) {
		
		m.addAttribute("title","User Dashboard");
		return "normal/user_dashboard";
	}
	
	@GetMapping("/add-contact")
	public String openAddContactForm(Model m) {
		
		m.addAttribute("title","Add Contact");
		m.addAttribute("contact",new Contact());
		return "normal/add_contact_form";
	}
	
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file,Principal principal,
			HttpSession session) {
		
		try {
			String name = principal.getName();
			User user = userRepository.getUserByUserName(name);
			contact.setUser(user);
			
			if(file.isEmpty()) {
				contact.setImage("contact.png");
			}
			else {
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			
			user.getContacts().add(contact);
			userRepository.save(user);
			
			session.setAttribute("message", new Message("Contact Added successfully.","success"));
		} catch (Exception e) {
			e.printStackTrace();
			session.setAttribute("message", new Message("Something went wrong","danger"));
		}
		
		
		return "normal/add_contact_form";
	}
	
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model m,Principal principal) {
		
		m.addAttribute("title","Your Contacts");
		
		String userName = principal.getName();
		User user = userRepository.getUserByUserName(userName);
		
		Pageable pageable = PageRequest.of(page, 3);
		Page<Contact> contacts = contactRepository.findContactsByUser(user.getId(),pageable);
		
		m.addAttribute("contacts",contacts);
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPages",contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	@GetMapping("/{cId}/contact/")
	public String showContactDetail(@PathVariable("cId") Integer cId,Model m,Principal principal) {
		
		Optional<Contact> contactOptional = contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		String userName = principal.getName();
		User user = userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId()) {
			m.addAttribute("contact",contact);
		}
		m.addAttribute("title","Contact Details");
		
		return "normal/contact_detail";
	}
	
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId,Model m,Principal principal,
			HttpSession session) throws IOException {
		
		Optional<Contact> contactOptional = contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		Contact oldContact = contact;
		
		String userName = principal.getName();
		User user = userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId()) {
			contact.setUser(null);
			contactRepository.deleteContactById(cId);
			File deleteFile = new ClassPathResource("static/img").getFile();
			File file2 = new File(deleteFile,oldContact.getImage());
			file2.delete();
			session.setAttribute("message", new Message("Contact deleted successfully","success"));
		}
		return "redirect:/user/show-contacts/0";
	}
	
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid,Model m) {
		m.addAttribute("title","Update Contact");
		Contact contact = contactRepository.findById(cid).get();
		m.addAttribute("contact",contact);
		return "normal/update_form";
	}
	
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file,
			Model m, HttpSession session, Principal principal) {
		
		try {
			Contact oldContact = contactRepository.findById(contact.getcId()).get();
			
			if(!file.isEmpty()) {
				
				//delete old pic
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file2 = new File(deleteFile,oldContact.getImage());
				file2.delete();
				
				//add new pic
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
			}
			else {
				contact.setImage(oldContact.getImage());
			}
			
			User user = userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:/user/"+contact.getcId()+"/contact/";
	}

	@GetMapping("/settings")
	public String openSettings(){
		return "normal/settings";
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword
			,@RequestParam("newPassword") String newPassword,Principal principal
			,HttpSession session){

		String userName = principal.getName();
		User currentUser = userRepository.getUserByUserName(userName);
		if(bCryptPasswordEncoder.matches(oldPassword,currentUser.getPassword())){
			currentUser.setPassword(bCryptPasswordEncoder.encode(newPassword));
			userRepository.save(currentUser);
			session.setAttribute("message", new Message("Your password is changed successfully.","success"));
		}
		else{
			//Logic for wrong password
			session.setAttribute("message", new Message("Please enter correct password","danger"));
			return "redirect:/user/settings";
		}

		return "redirect:/user/index";
	}

	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String, Object> data,Principal principal) throws RazorpayException {
		int amt = Integer.parseInt(data.get("amount").toString());
		var client = new RazorpayClient("rzp_test_Ux8gnwfhzTQJmK","KmNLQb9FVdXTTKxFS7SjzLaq");

		JSONObject ob = new JSONObject();
		ob.put("amount",amt*100);
		ob.put("currency","INR");
		ob.put("receipt","txn_235425");

		Order order = client.orders.create(ob);

		MyOrder myOrder = new MyOrder();
		myOrder.setAmount(order.get("amount")+"");
		myOrder.setOrderId(order.get("id"));
		myOrder.setPaymentId(order.get(null));
		myOrder.setStatus("created");
		myOrder.setUser(userRepository.getUserByUserName(principal.getName()));
		myOrder.setReceipt(order.get("receipt"));

		myOrderRepository.save(myOrder);

		return order.toString();

	}

	@PostMapping("/update_order")
	public ResponseEntity<?> updateOrder(@RequestBody Map<String,Object> data){

		MyOrder myOrder = myOrderRepository.findByOrderId(data.get("order_id").toString());
		myOrder.setPaymentId(data.get("payment_id").toString());
		myOrder.setStatus(data.get("status").toString());
		myOrderRepository.save(myOrder);

		return ResponseEntity.ok(Map.of("msg","updated"));
	}
}
