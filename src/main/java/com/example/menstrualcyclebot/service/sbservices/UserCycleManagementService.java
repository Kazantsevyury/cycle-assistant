package com.example.menstrualcyclebot.service.sbservices;


import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
@RequiredArgsConstructor
@Service
public class UserCycleManagementService {

    private final UserService userService;
    private final CycleService cycleService;


}
