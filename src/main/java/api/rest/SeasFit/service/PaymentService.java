package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.Payment;
import api.rest.SeasFit.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    public Payment save(Payment entity) {
        return paymentRepository.save(entity);
    }

    public void deleteById(Long id) {
        paymentRepository.deleteById(id);
    }
}
