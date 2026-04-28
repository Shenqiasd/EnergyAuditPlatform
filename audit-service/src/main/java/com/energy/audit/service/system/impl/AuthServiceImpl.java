package com.energy.audit.service.system.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.JwtUtils;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.enterprise.EntEnterpriseMapper;
import com.energy.audit.dao.mapper.system.SysUserMapper;
import com.energy.audit.model.dto.ChangePasswordDTO;
import com.energy.audit.model.dto.LoginDTO;
import com.energy.audit.model.entity.enterprise.EntEnterprise;
import com.energy.audit.model.entity.system.SysUser;
import com.energy.audit.model.vo.LoginVO;
import com.energy.audit.model.vo.UserInfoVO;
import com.energy.audit.service.system.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final SysUserMapper userMapper;
    private final EntEnterpriseMapper enterpriseMapper;
    private final CacheManager cacheManager;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public AuthServiceImpl(SysUserMapper userMapper, EntEnterpriseMapper enterpriseMapper, CacheManager cacheManager) {
        this.userMapper = userMapper;
        this.enterpriseMapper = enterpriseMapper;
        this.cacheManager = cacheManager;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        if (!ENCODER.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }

        // Validate portal matches user type
        if (dto.getPortal() != null) {
            int expectedType = switch (dto.getPortal()) {
                case "admin" -> 1;
                case "auditor" -> 2;
                case "enterprise" -> 3;
                default -> -1;
            };
            if (expectedType > 0 && !user.getUserType().equals(expectedType)) {
                throw new BusinessException("该账号无此门户的登录权限");
            }
        }

        String token = JwtUtils.generateToken(
                user.getId(), user.getUsername(), user.getUserType(), user.getEnterpriseId(),
                jwtSecret, jwtExpiration
        );

        userMapper.updateLastLoginTime(user.getId());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setUserType(user.getUserType());
        vo.setEnterpriseId(user.getEnterpriseId());
        if (user.getEnterpriseId() != null) {
            EntEnterprise ent = enterpriseMapper.selectById(user.getEnterpriseId());
            if (ent != null) {
                vo.setEnterpriseName(ent.getEnterpriseName());
            }
        }
        vo.setPasswordChanged(user.getPasswordChanged() != null && user.getPasswordChanged() == 1);

        log.info("User {} logged in successfully, type={}", user.getUsername(), user.getUserType());
        return vo;
    }

    @Override
    public void logout(String token) {
        // TODO: 生产环境如需高可用，应使用 Redis 持久化 token 黑名单，当前 Ehcache 内存缓存重启后失效
        if (token != null && cacheManager.getCache("tokenBlacklist") != null) {
            cacheManager.getCache("tokenBlacklist").put(token, Boolean.TRUE);
        }
        String username = SecurityUtils.getCurrentUsername();
        log.info("User {} logged out", username != null ? username : "(token-only)");
    }

    @Override
    public UserInfoVO getUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("未登录");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setUserType(user.getUserType());
        vo.setEnterpriseId(user.getEnterpriseId());
        if (user.getEnterpriseId() != null) {
            EntEnterprise ent = enterpriseMapper.selectById(user.getEnterpriseId());
            if (ent != null) {
                vo.setEnterpriseName(ent.getEnterpriseName());
            }
        }
        vo.setPasswordChanged(user.getPasswordChanged() != null && user.getPasswordChanged() == 1);
        vo.setAuditYear(java.time.LocalDate.now().getYear());
        return vo;
    }

    @Override
    public void changePassword(ChangePasswordDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!ENCODER.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().length() < 6) {
            throw new BusinessException("新密码长度不能少于6位");
        }
        String encoded = ENCODER.encode(dto.getNewPassword());
        userMapper.updatePassword(userId, encoded);
        log.info("User {} changed password", user.getUsername());
    }
}
