package com.github.mpalambonisi.syncup.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.mpalambonisi.syncup.exception.UserNotFoundException;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.TaskListRepository;
import com.github.mpalambonisi.syncup.repository.UserRepository;
import com.github.mpalambonisi.syncup.service.AccountService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final TaskListRepository taskListRepository;

    @Override
    @Transactional(readOnly = true)
    public User getAccountDetails(User user) {
        return userRepository.findById(user.getId())
        .orElseThrow(() -> new UserNotFoundException("User not found!"));
    }

    @Override
    public void deleteAccount(User user) {
       // Find all task lists where user is a collaborator
       List<TaskList> allTaskLists = taskListRepository.findAll();

       for (TaskList taskList: allTaskLists){
        // Remove user from collaborators if they are in the list
        if (taskList.getCollaborators().contains(user)) {
            taskList.getCollaborators().remove(user);
            taskListRepository.save(taskList);
        }
       }

       // Delete all task lists owned by the user (cascade will handle tasks)
       List<TaskList> ownedTaskLists = taskListRepository.findAllByOwner(user);
       taskListRepository.deleteAll(ownedTaskLists);

       // Finally, delete the user account
       userRepository.deleteById(user.getId());
    }
}
