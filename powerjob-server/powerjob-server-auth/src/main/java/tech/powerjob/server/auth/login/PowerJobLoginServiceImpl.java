package tech.powerjob.server.auth.login;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.login.biz.BizLoginService;
import tech.powerjob.server.auth.login.biz.BizUser;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PowerJobLoginService
 *
 * @author tjq
 * @since 2023/3/21
 */
@Slf4j
@Service
public class PowerJobLoginServiceImpl implements PowerJobLoginService {

    private final UserInfoRepository userInfoRepository;
    private final Map<String, BizLoginService> type2LoginService = Maps.newHashMap();

    private static final String TOKEN_HEADER_NAME = "power_token";

    @Autowired
    public PowerJobLoginServiceImpl(List<BizLoginService> loginServices, UserInfoRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
        loginServices.forEach(k -> type2LoginService.put(k.type(), k));
    }


    @Override
    public Optional<PowerJobUser> login(LoginContext loginContext) {
        final String loginType = loginContext.getLoginType();
        final BizLoginService loginService = type2LoginService.get(loginType);
        if (loginService == null) {
            throw new IllegalArgumentException("can't find LoginService by type: " + loginType);
        }
        final Optional<BizUser> bizUserOpt = loginService.login(loginContext);
        if (!bizUserOpt.isPresent()) {
            return Optional.empty();
        }
        final BizUser bizUser = bizUserOpt.get();

        String dbUserName = String.format("%s_%s", loginType, bizUser.getUsername());
        final Optional<UserInfoDO> powerJobUserOpt = userInfoRepository.findByUsername(dbUserName);

        PowerJobUser ret = new PowerJobUser();
        // 存在则响应 PowerJob 用户
        if (powerJobUserOpt.isPresent()) {
            final UserInfoDO dbUser = powerJobUserOpt.get();
            BeanUtils.copyProperties(dbUser, ret);
            ret.setUsername(dbUserName);
            return Optional.of(ret);
        }

        // 同步在 PowerJob 用户库创建该用户
        UserInfoDO newUser = new UserInfoDO();
        newUser.setUsername(dbUserName);
        log.info("[PowerJobLoginService] sync user to PowerJobUserSystem: {}", dbUserName);
        userInfoRepository.saveAndFlush(newUser);
        ret.setUsername(dbUserName);

        return Optional.of(ret);
    }

    @Override
    public Optional<PowerJobUser> parse(LoginContext loginContext) {

        final Optional<String> usernameOpt = parseUsername(loginContext);
        if (!usernameOpt.isPresent()) {
            return Optional.empty();
        }
        return userInfoRepository.findByUsername(usernameOpt.get()).map(userInfoDO -> {
            PowerJobUser powerJobUser = new PowerJobUser();
            BeanUtils.copyProperties(usernameOpt, powerJobUser);
            return powerJobUser;
        });
    }

    private Optional<String> parseUsername(LoginContext loginContext) {
        final HttpServletRequest httpServletRequest = loginContext.getHttpServletRequest();
        final String tokenHeader = httpServletRequest.getHeader(TOKEN_HEADER_NAME);
        if (StringUtils.isEmpty(tokenHeader)) {
            return Optional.empty();
        }

        // TODO: 从 jwt token 解析 username
        return Optional.empty();
    }
}
