package cn.tedu.store.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cn.tedu.store.controller.exception.FileEmptyException;
import cn.tedu.store.controller.exception.FileSizeOutOfLimitException;
import cn.tedu.store.controller.exception.FileTypeNotSupportException;
import cn.tedu.store.controller.exception.FileUploadException;
import cn.tedu.store.controller.exception.RequestException;
import cn.tedu.store.entity.User;
import cn.tedu.store.service.IUserService;
import cn.tedu.store.service.exception.ServiceException;
import cn.tedu.store.util.ResponseResult;

@RestController
@RequestMapping("/user")
public class UserController extends BaseController {
	//文件名
	private static final String UPLOAD_DIR_NAME="upload";
	//文件大小
	private static final long FILE_MAX_SIZE = 5*1024*1024;
	//文件类型集合
	private static final List<String> FILE_CONTENT_TYPES = new ArrayList<String>();
	//初始化允许上传的文件类型的集合
	static {
		FILE_CONTENT_TYPES.add("image/jpg");
		FILE_CONTENT_TYPES.add("image/jpeg");
		FILE_CONTENT_TYPES.add("image/png");
	}
	@Autowired
	private IUserService userService;
	
	@PostMapping("/reg.do")
	public ResponseResult<Void> handleReg(
			User user) {
		userService.reg(user);
		return new ResponseResult<Void>(SUCCESS);
	}

	@PostMapping("/login.do")
	public ResponseResult<Void> handleLogin(
		@RequestParam("username") String username,
		@RequestParam("password") String password,
		HttpSession session) {
		// 执行登录
		User user
			= userService.login(username, password);
		// 将相关信息存入到Session
		session.setAttribute("uid", user.getId());
		session.setAttribute("username", user.getUsername());
		// 返回
		return new ResponseResult<>(SUCCESS);
	}
	
	@RequestMapping("/password.do")
	public ResponseResult<Void> changePassword(
		@RequestParam("old_password") String oldPassword,
		@RequestParam("new_password") String newPassword,
		HttpSession session) {
		// 获取当前登录的用户的id
		Integer uid = getUidFromSession(session);
		// 执行修改密码
		userService.changePassword(
				uid, oldPassword, newPassword);
		// 返回
		return new ResponseResult<>(SUCCESS);
	}
	
	@RequestMapping("/info.do")
	public ResponseResult<User> getInfo(
		HttpSession session) {
		// 获取当前登录的用户的id
		Integer id = getUidFromSession(session);
		// 执行查询，获取用户数据
		User user = userService.getById(id);
		// 返回
		return new ResponseResult<User>(SUCCESS, user);
	}
	
	@PostMapping("/change_info.do")
	public ResponseResult<Void> changeInfo(
	    User user, HttpSession session) {
		// 获取当前登录的用户的id
		Integer id = getUidFromSession(session);
		// 将id封装到参数user中，因为user是用户提交的数据，并不包含id
		user.setId(id);
		// 执行修改
		userService.changeInfo(user);
		// 返回
		return new ResponseResult<>(SUCCESS);
	}
	
	
	@PostMapping("/upload.do")
	public ResponseResult<String> handleUpload(
			@RequestParam("file") MultipartFile file,
			HttpSession session){
		
		//TODO 检查是否存在上传文件
		if(file.isEmpty()){
			//抛出异常，文件不允许为空
			throw new FileEmptyException("文件不允许为空");
		}
		//TODO 检查文件大小
		if(file.getSize() > FILE_MAX_SIZE){
			//抛出异常，文件大小超出限制
			throw new FileSizeOutOfLimitException("文件大小超出限制");
		}
		//TODO 检查文件类型
		if(!FILE_CONTENT_TYPES.contains(file.getContentType())){
			//抛出异常，文件类型限制
			throw new FileTypeNotSupportException("文件类型限制");
		}
		
		//确定上传文件的路径
		String parentPath = session.getServletContext().getRealPath(UPLOAD_DIR_NAME);
		File parent = new File(parentPath);
		if(!parent.exists()){
			parent.mkdirs();
		}
		System.out.println(parentPath);
		//确定文件名
		String originalFileName = file.getOriginalFilename();
		int beginIndex = originalFileName.lastIndexOf(".");
		String suffix = originalFileName.substring(beginIndex);
		String fileName = System.currentTimeMillis()+""+(new Random().nextInt(900000)+100000)+suffix;
		File dest = new File(parent,fileName);
		System.out.println(dest);
		//保存文件
		try {
			file.transferTo(dest);
		} catch (IllegalStateException e) {
			//抛出异常，上传失败
			throw new FileUploadException("上传失败");
		} catch (IOException e) {
			//抛出异常，上传失败
			throw new FileUploadException("上传失败");
		}
		
		Integer uid = getUidFromSession(session);
		String avatar = "/"+UPLOAD_DIR_NAME+"/"+fileName;
		userService.changeAvatar(uid, avatar);
		
		
		ResponseResult<String> rr = new ResponseResult<String>();
		rr.setState(SUCCESS);
		rr.setData("/"+UPLOAD_DIR_NAME+"/"+fileName);
		return rr;
	}
	
	
}







