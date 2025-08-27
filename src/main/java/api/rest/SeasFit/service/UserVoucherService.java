// api/rest/SeasFit/service/UserVoucherService.java
package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.UserVoucherDTO;
import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.entity.UserVoucher;
import api.rest.SeasFit.entity.Voucher;
import api.rest.SeasFit.repository.UserRepository;
import api.rest.SeasFit.repository.UserVoucherRepository;
import api.rest.SeasFit.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserVoucherService {

    private final UserRepository userRepository;
    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;

    @Transactional
    public void claim(Long userId, String voucherCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Voucher voucher = voucherRepository.findByCode(voucherCode)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        if (userVoucherRepository.existsByUser_IdAndVoucher_Code(userId, voucherCode)) {
            throw new IllegalStateException("Bạn đã có voucher này");
        }

        userVoucherRepository.save(
                UserVoucher.builder()
                        .user(user)
                        .voucher(voucher)
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public List<UserVoucherDTO> listForUser(Long userId) {
        return userVoucherRepository.findAllForUserDTO(userId);
    }

    @Transactional
    public void revoke(Long userId, String voucherCode) {
        var uv = userVoucherRepository.findByUser_IdAndVoucher_Code(userId, voucherCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher trong ví"));
        userVoucherRepository.delete(uv);
    }
}
