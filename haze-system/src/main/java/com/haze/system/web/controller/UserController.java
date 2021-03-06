package com.haze.system.web.controller;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.haze.system.entity.Group;
import com.haze.system.entity.Role;
import com.haze.system.entity.User;
import com.haze.system.service.GroupService;
import com.haze.system.service.RoleService;
import com.haze.system.service.UserService;
import com.haze.system.utils.Sex;
import com.haze.system.utils.Status;
import com.haze.web.BaseCrudController;
import com.haze.web.utils.WebMessage;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


/**
 * 系统管理--用户管理Controller
 * @author sofar
 *
 */
@Controller
@RequestMapping(value = "/system/user")
public class UserController extends BaseCrudController<User, Long> {

	private UserService userService;

	private RoleService roleService;

	private GroupService groupService;

	public UserController(UserService userService, RoleService roleService, GroupService groupService) {
		super("system", "user", "用户信息", userService);
		this.userService = userService;
		this.roleService = roleService;
		this.groupService = groupService;
	}

	@Override
	protected void setPageQueryVariables(Map<String, Object> queryVariables, HttpServletRequest request) {
		if (queryVariables != null && queryVariables.get("status") != null) {
			String value = (String) queryVariables.get("status");
			//将传递进来的status字符串转化为Status枚举对象
			queryVariables.put("status", Status.valueOf(value));
		}
		if (queryVariables != null && queryVariables.get("userType") != null) {
			String value = (String) queryVariables.get("userType");
			//将传递进来的status字符串转化为Status枚举对象
//			queryVairables.put("userType", UserType.valueOf(value));
		}
		if (queryVariables.get("group.id") != null) {
			Long groupId = Long.valueOf(queryVariables.get("group.id").toString()) ;
			queryVariables.put("group.id",groupId);
		}
		queryVariables.put("loginName_notEq", User.ADMIN);
	}

	@Override
	protected void setModel(Model model, HttpServletRequest request) {
		model.addAttribute("statuss", Status.values());
		model.addAttribute("groupId", request.getParameter("groupId"));
	}

	/**
	 * 进入添加用户页面
	 * @param model
	 * @return 添加用户页面
	 */
	@Override
	@GetMapping(value = "add")
	public String add(Model model, HttpServletRequest request) {
		List<Role> roleList = this.roleService.findByEnabled(true); //查找所有启用状态的角色
		model.addAttribute("roleList", roleList);
		model.addAttribute("sexs", Sex.values());
		model.addAttribute("statuss", Status.values());
		String parentId = request.getParameter("parentId");
		if (parentId != null) {
			model.addAttribute("group", groupService.findById(Long.parseLong(parentId)));
		}
		return "system/user/add";
	}
	
	/**
	 * 保存用户，同时保存用户角色关联信息
	 * @param user 用户对象
	 * @param roleIds 角色ID集合
	 * @return 返回用户列表页面
	 */
	@PostMapping(value = "saveUser")
    @ResponseBody
	public WebMessage save(User user, Long[] roleIds) {
		Set<Role> roles = new HashSet<>();
		if (roleIds != null) {
			for (Long roleId : roleIds) {
				Role role = new Role();
				role.setId(roleId);
				roles.add(role);
			}
			if (!roles.isEmpty()) {
				user.setRoles(roles);
			}
		}
        try {
            this.userService.saveOrUpdate(user);
            return WebMessage.createSuccessWebMessage();
        } catch (Exception e) {
        	logger.error("用户添加/更新失败", e);
            return WebMessage.createErrorWebMessage(e.getMessage());
        }
    }
	
	/**
	 * 进入用户添加角色页面
	 * @param id 用户ID
	 * @param model
	 * @return 用户添加角色页面
	 */
	@GetMapping(value = "addRoles/{id}")
	public String addRoles(@PathVariable("id")Long id, Model model) {
		User user = this.userService.findById(id);
		List<Role> roleList = this.roleService.findByEnabled(true);
		Group group = user.getGroup();
		if(group != null){
			model.addAttribute("groupId", group.getId());
		}
		model.addAttribute("user", user);
		roleList.removeAll(user.getRoles());
		model.addAttribute("roleList",roleList);
		return "/system/user/addUserRole";
	}
	
	/**
	 * 对用户进行角色授权
	 * @param roleIds 角色ID集合
	 * @param id 用户ID
	 * @return 返回用户列表页面
	 */
	@PostMapping(value = "saveRoles")
    @ResponseBody
	public WebMessage saveRoles(@RequestParam(value="roleIds",required=false) Long[] roleIds, @RequestParam("id") Long id) {
		Set<Role> roles = new HashSet<Role>();
		if (null != roleIds) {
			for (Long roleId :roleIds) {
				Role role = new Role();
				role.setId(roleId);
				roles.add(role);
			}
		}
        try {
            this.userService.addRoles(id, roles);
            return WebMessage.createSuccessWebMessage();
        } catch (Exception e) {
            return WebMessage.createErrorWebMessage(e.getMessage());
        }
	}
	
	/**
	 * 更改用户密码
	 * @author 王先先  修改  2013-11-28   先判断用户输入的旧密码是否正确，正确的情况下才可修改密码
	 * @param id    用户id
	 * @param newPassword  新密码
	 * @return
	 */
	@PostMapping(value = "updatePassword")
	@ResponseBody
	public String updatePassword(@RequestParam(value = "id") Long id,@RequestParam(value = "newPassword") String newPassword) {
		try {
			this.userService.updatePassword(id, newPassword);
			return WebMessage.ACTION_SUCCESS_MESSAGE;
		} catch (Exception e) {
			return e.getMessage();
		}
	}
	
	@PostMapping(value = "resetPassword/{id}")
	public WebMessage resetPassword(@PathVariable Long id, Model model) {
        try {
            this.userService.resetPassword(id);
            return WebMessage.createSuccessWebMessage();
        } catch (Exception e) {
            return WebMessage.createErrorWebMessage(e.getMessage());
        }
    }
	
	/**
	 * 进入用户编辑页面
	 * @param id 用户Id
	 * @param model
	 * @return
	 */
	@Override
	@GetMapping(value = "edit/{id}")
	public String edit(@PathVariable Long id, Model model, HttpServletRequest request) {
		model.addAttribute("sexs", Sex.values());
		model.addAttribute("statuss", Status.values());
		List<Group> groupList = this.groupService.findAll();
		model.addAttribute("user", this.userService.findById(id));
		model.addAttribute("groupList",groupList);
		List<Role> roleList = this.roleService.findByEnabled(true); //查找所有启用状态的角色
		model.addAttribute("roleList", roleList);
		return "system/user/edit";
	}
	
	/**
	 * 更新用户信息
	 * @param user 用户
	 * @return 返回用户列表页面
	 */
	@PostMapping(value = "update")
	@ResponseBody
	public WebMessage update(User user) {
		User u = this.userService.findById(user.getId());
		u.setName(user.getName());
		u.setEmail(user.getEmail());
		u.setSex(user.getSex());
		u.setStatus(user.getStatus());
		if (user.getGroup() == null || user.getGroup().getId()!= null) {
			u.setGroup(null);
		} else {
			u.setGroup(user.getGroup());
		}
		u.setMobile(user.getMobile());
		u.setTel(user.getTel());
		u.setRoles(user.getRoles());
        try {
            this.userService.saveOrUpdate(u);
            return WebMessage.createSuccessWebMessage();
        } catch (Exception e) {
            return WebMessage.createErrorWebMessage(e.getMessage());
        }
	}
	
	/**
	 * 判断登录名是否存在
	 * @param loginName 登录名
	 * @return true/false
	 */
	@PostMapping(value = "isNotExistLoginName")
	@ResponseBody
	public Boolean isNotExistLoginName(String loginName) {
		boolean isExist = this.userService.existLoginName(loginName);
		return !isExist;
	}	
}
