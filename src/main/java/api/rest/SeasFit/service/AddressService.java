package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.AddressRequest;
import api.rest.SeasFit.dto.AddressResponse;
import api.rest.SeasFit.entity.Address;
import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.repository.AddressRepository;
import api.rest.SeasFit.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;


    public List<AddressResponse> getAllByUser(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }
    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        // Nếu là địa chỉ đầu tiên → mặc định là true
        boolean isFirst = addressRepository.countByUserId(userId) == 0;

        if (Boolean.TRUE.equals(request.getIsDefault()) || isFirst) {
            addressRepository.clearDefaultForUser(userId); // Xoá mặc định cũ nếu có
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Address address = new Address();
        address.setUser(user);
        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setStreet(request.getStreet());
        address.setCountry(request.getCountry() != null ? request.getCountry() : "Việt Nam");
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()) || isFirst);
        address.setCreatedAt(LocalDateTime.now());

        addressRepository.save(address);

        return toResponse(address);
    }



    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ"));

        // Nếu được chọn là mặc định → clear các địa chỉ khác
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultForUser(userId);
        }

        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setStreet(request.getStreet());
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));

        addressRepository.save(address);

        // Nếu không còn địa chỉ nào là mặc định, gán mặc định cho 1 địa chỉ bất kỳ
        boolean hasDefault = addressRepository.existsByUserIdAndIsDefaultTrue(userId);
        if (!hasDefault) {
            addressRepository.findFirstByUserIdOrderByCreatedAtAsc(userId)
                    .ifPresent(first -> {
                        first.setIsDefault(true);
                        addressRepository.save(first);
                    });
        }

        AddressResponse response = new AddressResponse();
        response.setId(address.getId());
        response.setFullName(address.getFullName());
        response.setPhone(address.getPhone());
        response.setCity(address.getCity());
        response.setDistrict(address.getDistrict());
        response.setWard(address.getWard());
        response.setStreet(address.getStreet());
        response.setCountry(address.getCountry());
        response.setIsDefault(address.getIsDefault());

        return response;
    }



    public void delete(Long userId, Long id) {
        Address address = addressRepository.findById(id)
                .filter(a -> a.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);

        if (wasDefault) {
            addressRepository.findFirstByUserIdOrderByCreatedAtAsc(userId)
                    .ifPresent(first -> {
                        first.setIsDefault(true);
                        addressRepository.save(first);
                    });
        }
    }



    private AddressResponse toResponse(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .fullName(a.getFullName())
                .phone(a.getPhone())
                .street(a.getStreet())
                .ward(a.getWard())
                .district(a.getDistrict())
                .city(a.getCity())
                .country(a.getCountry())
                .isDefault(a.getIsDefault())
                .build();
    }
}
