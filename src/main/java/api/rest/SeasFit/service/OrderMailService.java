package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.Order;
import api.rest.SeasFit.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class OrderMailService {

    private final MailService mailService;

    @Value("${frontend.url}")
    private String frontendUrl;

    private BigDecimal toMoney(Number n) {
        if (n == null) return BigDecimal.ZERO;
        if (n instanceof BigDecimal bd) return bd;
        return BigDecimal.valueOf(n.longValue());
    }
    private BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private String format(BigDecimal v) { return v == null ? "0" : String.format("%,d", v.longValue()); }
    private String safe(Object o) { return o == null ? "" : o.toString(); }

    private String buildPublicLink(Order order) {
        long key = order.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        return String.format("%s/check-order?orderId=%s&key=%s", frontendUrl, order.getId(), key);
    }

    /** Gửi mail xác nhận đơn hàng HTML (có key timestamp) */
    public void sendOrderConfirmation(User user, Order order) {
        BigDecimal total    = toMoney(order.getTotalAmount());
        BigDecimal discount = nvl(order.getDiscountAmount());
        BigDecimal ship     = toMoney(order.getShippingFee());
        BigDecimal payable  = total.subtract(discount).add(ship);

        String subject = "🛍️ Xác nhận đơn hàng #" + order.getId();
        String link    = buildPublicLink(order); // ✅ link có &key=

        String html = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8">
                  <style>
                    body { font-family: Arial, sans-serif; background:#f4f4f7; padding:20px; }
                    .card { max-width:600px; margin:auto; background:#fff; border-radius:12px; padding:24px;
                            box-shadow:0 4px 10px rgba(0,0,0,0.1); }
                    .header { text-align:center; margin-bottom:20px; }
                    .header h2 { margin:0; color:#111; }
                    .order-info { margin:20px 0; }
                    .order-info p { margin:6px 0; font-size:14px; color:#333; }
                    .highlight { font-weight:bold; color:#2563eb; }
                    .total { font-size:18px; font-weight:bold; color:#dc2626; margin-top:12px; }
                    .footer { margin-top:30px; font-size:13px; color:#666; text-align:center; }
                    .btn { display:inline-block; margin-top:20px; padding:12px 20px; 
                           background:#2563eb; color:white; text-decoration:none;
                           border-radius:8px; font-weight:bold; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <div class="header">
                      <h2>🛍️ Cảm ơn bạn đã đặt hàng tại <span class="highlight">SeasFit</span></h2>
                      <p>Xác nhận đơn hàng <b>#{0}</b></p>
                    </div>
                    <div class="order-info">
                      <p>Xin chào <b>{1}</b>,</p>
                      <p>Trạng thái: <span class="highlight">{2}</span></p>
                      <p>Tổng tiền: {3}₫</p>
                      <p>Giảm giá: -{4}₫</p>
                      <p>Phí vận chuyển: {5}₫</p>
                      <p class="total">Thành tiền: {6}₫</p>
                    </div>
                    <a href="{ORDER_LINK}" class="btn">Xem chi tiết đơn hàng</a>
                    <div class="footer">
                      <p>Chúng tôi sẽ sớm xử lý và gửi hàng cho bạn.</p>
                      <p>Trân trọng,<br/>Đội ngũ SeasFit</p>
                    </div>
                  </div>
                </body>
                </html>
                """
                .replace("{ORDER_LINK}", link)                 // ✅ dùng link có key
                .replace("{0}", String.valueOf(order.getId()))
                .replace("{1}", safe(user.getFullName()))
                .replace("{2}", safe(order.getStatus()))
                .replace("{3}", format(total))
                .replace("{4}", format(discount))
                .replace("{5}", format(ship))
                .replace("{6}", format(payable));

        mailService.sendHtml(user.getEmail(), subject, html);
    }
}
