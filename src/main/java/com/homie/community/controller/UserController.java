package com.homie.community.controller;

import com.homie.community.Annotation.LoginRequired;
import com.homie.community.entity.User;
import com.homie.community.service.FollowService;
import com.homie.community.service.LikeService;
import com.homie.community.service.UserService;
import com.homie.community.util.CommunityConstant;
import com.homie.community.util.CommunityUtil;
import com.homie.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {
    private  static  final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${community.path.domain}")
    private String domain;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private LikeService likeService;
    @Autowired
    private FollowService followService;
    @Value("${qiniu.key.access}")
    private String accessKey;
    @Value("${qiniu.key.secret}")
    private String secretKey;
    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;
    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;


    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {
        // ??????????????????
        String fileName = CommunityUtil.generateUUID();
        // ??????????????????
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0));
        // ??????????????????
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);

        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);

        return "/site/setting";
    }

    // ??????????????????
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "?????????????????????!");
        }

        String url = headerBucketUrl + "/" + fileName;
        userService.updateHeaderUrl(hostHolder.getUsers().getId(), url);

        return CommunityUtil.getJSONString(0);
    }

    //??????
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeaderUrl(MultipartFile headerImage, Model  model){
        //?????????????????????????????????
        if(headerImage==null){
            model.addAttribute("error","???????????????????????????");
            return "/site/setting";
        }
        String filename = headerImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf(".")+1);
        //????????????????????????
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error","????????????????????????");
            return "/site/setting";
        }
        //????????????????????????
        filename = CommunityUtil.generateUUID()+"."+suffix;
        //????????????????????????
        File dest = new File(uploadPath+"/"+filename);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("?????????????????????"+e.getMessage());
            throw new RuntimeException("?????????????????????????????????????????????",e);
        }
        //??????????????????????????????(web????????????)
        //http???localhost8080/community/user/header/xxx.png
        User user = hostHolder.getUsers();
        String headerUrl = domain + contextPath + "/user/header/"+filename;
        userService.updateHeaderUrl(user.getId(),headerUrl);
        return "redirect:/index";
    }


    //??????
    @RequestMapping(path = "/header/{filename}",method = RequestMethod.GET)
   //???????????????????????????????????????????????????????????????????????????????????????
    public void getHeader(@PathVariable("filename") String filename, HttpServletResponse response){
        //????????????????????????
        filename = uploadPath + "/" + filename;
        //???????????????
        String suffix = filename.substring(filename.lastIndexOf(".")+1);
        //????????????
        response.setContentType("image/"+suffix);
        try(
                //?????????????????????????????????
                FileInputStream fis = new FileInputStream(filename);
                ) {
            //?????????springmvc????????????
            OutputStream os = response.getOutputStream();
            byte[] buffer   = new byte[1024];
            int b = 0;
            while((b=fis.read(buffer))!=-1){
                os.write(buffer,0,b);
            }
        } catch (IOException e) {
            logger.error("??????????????????"+e.getMessage());
        }
    }
    // ????????????
    @RequestMapping(path = "/updatePassword", method = RequestMethod.POST)
    public String updatePassword(String oldPassword, String newPassword, Model model) {
        User user = hostHolder.getUsers();
        Map<String, Object> map = userService.updatePassword(user.getId(), oldPassword, newPassword);
        if (map == null || map.isEmpty()) {
            return "redirect:/logout";
        } else {
            model.addAttribute("oldPasswordMsg", map.get("oldPasswordMsg"));
            model.addAttribute("newPasswordMsg", map.get("newPasswordMsg"));
            return "/site/setting";
        }
    }
    //????????????
    @RequestMapping(path = "/profile/{userId}",method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId")int userId,Model model){
        User user = userService.findUserById(userId);
        if(user==null){
            throw new RuntimeException("??????????????????");
        }
        model.addAttribute("user",user);
        //????????????
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount",likeCount);
        //??????????????????
        long followeeCount = followService.findFolloweeCount(userId,ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);
        //???????????????
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER,userId);
        model.addAttribute("followerCount",followerCount);
        //?????????????????????????????????
        boolean hasFollowed = false;
        if(hostHolder.getUsers()!=null){
            hasFollowed = followService.hasFollowed(hostHolder.getUsers().getId(),ENTITY_TYPE_USER,userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);
        return "/site/profile";
    }
}
