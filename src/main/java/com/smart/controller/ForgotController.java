package com.smart.controller;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Properties;
import java.util.Random;

@Controller
public class ForgotController {

    Random random = new Random(1000);

    @Autowired
    UserRepository userRepository;

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;
    @RequestMapping("/forgot")
    public String openEmailForm(){
        return "forgot_email_form";
    }

    @PostMapping("/send-otp")
    public String sendOTP(@RequestParam("email") String email, HttpSession session){

        int otp = random.nextInt(999999)%10000;
        System.out.println(otp);
        //sendEmail();

        session.setAttribute("myotp",otp);
        session.setAttribute("email",email);
        return "verify_otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam("otp") int otp,HttpSession session){
        int myOtp = (int)session.getAttribute("myotp");
        String email = (String)session.getAttribute("email");
        if(myOtp==otp){
            User user = userRepository.getUserByUserName(email);
            if(user==null){
//                session.setAttribute("message", new Message("Enter correct email","danger"));
                return "forgot_email_form";
            }
            else {
                return "password_change_form";
            }
        }
        else{
//            session.setAttribute("message", new Message("You have entered wrong otp","danger"));
            return "verify_otp";
        }
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam("newpassword") String newpassword,HttpSession session){
        String email = (String)session.getAttribute("email");
        User user = userRepository.getUserByUserName(email);
        user.setPassword(bCryptPasswordEncoder.encode(newpassword));
        userRepository.save(user);
        return "redirect:/signin";
    }

    private void sendEmail(String toEmail,String subject,String body) {
    }
}
