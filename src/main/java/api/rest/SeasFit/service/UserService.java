package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepo;

    public User findByUserName(String userName) {
        return userRepo.findByUserName(userName);
    }

    public User findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public User findByPhone(String phone) {
        return userRepo.findByPhone(phone);
    }

    public int getTotalUser() {
        return (int) userRepo.count();
    }

    public User findById(Long id) {
        return userRepo.findById(id).orElse(null);
    }

    public void save(User user) {
        // Bỏ kiểm tra phone/email nếu null (đăng nhập OAuth không có phone)
        if (!userRepo.existsByUserName(user.getUserName())) {
            userRepo.save(user);
        }
    }

    public User update(User user) {
        return userRepo.save(user);
    }

    public void deleteById(Long id) {
        userRepo.deleteById(id);
    }

    public boolean existsByUsername(String userName) {
        return userRepo.existsByUserName(userName);
    }

    public boolean existsByEmail(String email) {
        return email != null && userRepo.existsByEmail(email);
    }

    public boolean existsByPhone(String phone) {
        return phone != null && userRepo.existsByPhone(phone);
    }

    public String validateUser(User user) {
        if (existsByUsername(user.getUserName())) return "Tên đăng nhập đã tồn tại!";
        if (existsByEmail(user.getEmail())) return "Email đã tồn tại!";
        if (existsByPhone(user.getPhone())) return "Số điện thoại đã tồn tại!";
        return null;
    }
}
