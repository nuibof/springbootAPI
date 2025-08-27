package api.rest.SeasFit.controller;

import api.rest.SeasFit.service.ProductService;
import api.rest.SeasFit.service.VoucherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RestTemplate rest = new RestTemplate();
    private final ProductService productService;
    private final VoucherService voucherService;

    public ChatController(ProductService productService, VoucherService voucherService) {
        this.productService = productService;
        this.voucherService = voucherService;
    }

    @Value("${openai.api.key:#{null}}")
    private String apiKeyProp;

    private String apiKey() {
        String k = apiKeyProp;
        if (k == null || k.isBlank()) k = System.getenv("OPENAI_API_KEY");
        if (k == null || k.isBlank()) throw new RuntimeException("OPENAI_API_KEY not set");
        return k;
    }

    private static final String SYSTEM_BASE = String.join("\n",
            "Bạn là trợ lý ảo của cửa hàng thời trang trực tuyến 'SeasFit'.",
            "Stack kỹ thuật: Spring Boot (backend) + Vue 3 (frontend).",
            "Thông tin cửa hàng:",
            "- SeasFit chuyên bán quần áo thời trang nam nữ: áo thun, sơ mi, hoodie, váy, quần jeans, đồ tập thể thao.",
            "- Phong cách trẻ trung, năng động, hợp Gen Z.",
            "- Có phân loại sản phẩm theo giới tính (nam/nữ/unisex), màu sắc, size.",
            "- Thường xuyên có voucher khuyến mãi và chương trình flash sale.",
            "- Các sản phẩm có nhiều phiên bản (variant: màu, size).",
            "- Hệ thống hiển thị sản phẩm bán chạy, yêu thích, mới về, đang giảm giá.",
            "- Giao hàng toàn quốc, thanh toán COD hoặc ví điện tử (MoMo, VNPay...).",
            "- Chính sách đổi trả trong 7 ngày nếu lỗi hoặc không vừa size.",
            "Nguyên tắc khi trả lời:",
            "- Luôn dùng tiếng Việt, giọng Gen Z vui vẻ, tối đa 4 câu.",
            "- Khi có BESTSELLERS_HTML / FAVORITES_HTML / VOUCHERS_HTML thì chèn NGUYÊN block vào câu trả lời.",
            "- Không bịa voucher hay sản phẩm ngoài dữ liệu được cung cấp."
    );


    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> body) {
        String userMsg = Objects.toString(body.get("message"), "").trim();
        if (userMsg.isEmpty()) return java.util.Map.of("reply", "Nhập câu hỏi đi bro 😅");

        // intent detect
        String lower = userMsg.toLowerCase(Locale.ROOT);
        boolean askBest    = lower.contains("bán chạy") || lower.contains("bestseller") || lower.contains("phổ biến") || lower.contains("hot");
        boolean askFav     = lower.contains("yêu thích") || lower.contains("favorite");
        boolean askVoucher = lower.contains("mã") || lower.contains("voucher") || lower.contains("giảm giá");

        // ===== build system prompt trước rồi mới gọi OpenAI =====
        String system = SYSTEM_BASE;

        if (askVoucher) {
            var list = voucherService.getActiveVouchers();

            // Đẩy JSON full cho model xử lý logic
            system += "\nVOUCHERS=" + toJson(list);

            // HTML để FE render (có format %/VNĐ, min order, số lượt, HSD)
            StringBuilder html = new StringBuilder("Các voucher đang hoạt động:<ul>");
            for (var v : list) {
                String code = String.valueOf(v.get("code"));
                String desc = Objects.toString(v.get("description"), "");
                String type = String.valueOf(v.get("type"));        // AMOUNT | PERCENT
                Long value  = v.get("value")    == null ? null : ((Number) v.get("value")).longValue();
                Long maxVal = v.get("maxValue") == null ? null : ((Number) v.get("maxValue")).longValue();
                Long minOrd = v.get("minOrder") == null ? null : ((Number) v.get("minOrder")).longValue();
                Integer qty = v.get("quantity") == null ? null : ((Number) v.get("quantity")).intValue();
                String end  = Objects.toString(v.get("endDate"), "");

                html.append("<li><b>")
                        .append(escape(code))
                        .append("</b> – ")
                        .append(escape(desc))
                        .append(" • ");

                if ("PERCENT".equalsIgnoreCase(type)) {
                    html.append("Giảm ").append(value).append("%");
                    if (maxVal != null) html.append(" (tối đa ").append(formatVnd(maxVal)).append(")");
                } else {
                    html.append("Giảm ").append(formatVnd(value));
                }

                if (minOrd != null) html.append(" • ĐH tối thiểu ").append(formatVnd(minOrd));
                if (qty != null)    html.append(" • Còn ").append(qty).append(" lượt");
                if (!end.isBlank()) html.append(" • HSD: ").append(end);

                html.append("</li>");
            }
            html.append("</ul>");
            system += "\nVOUCHERS_HTML=\n" + html;
        }


        if (askBest) {
            var list = productService.getBestsellers(30, 8); // 30 ngày gần nhất
            StringBuilder htmlList = new StringBuilder("Đây là một số sản phẩm bán chạy nè:<ul>");
            for (var b : list) {
                htmlList.append("<li>")
                        .append("<a class=\"text-blue-600 hover:underline\" href=\"/product/")
                        .append(b.getId())
                        .append("\">")
                        .append(escape(b.getName()))
                        .append("</a> (")
                        .append(b.getTotalSold())
                        .append(" lượt mua)")
                        .append("</li>");
            }
            htmlList.append("</ul>");
            system += "\nBESTSELLERS_HTML=\n" + htmlList;
        }

        if (askFav) {
            var list = productService.getMostFavorited(8);
            StringBuilder htmlList = new StringBuilder("Đây là những sản phẩm được yêu thích nhất:<ul>");
            for (var f : list) {
                htmlList.append("<li>")
                        .append("<a class=\"text-blue-600 hover:underline\" href=\"/product/")
                        .append(f.getId())
                        .append("\">")
                        .append(escape(f.getName()))
                        .append("</a> (")
                        .append(f.getTotalFavorites())
                        .append(" lượt thích)")
                        .append("</li>");
            }
            htmlList.append("</ul>");
            system += "\nFAVORITES_HTML=\n" + htmlList;
        }

        // ===== call OpenAI =====
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey());

        // messages
        java.util.List<java.util.Map<String, Object>> messages = new ArrayList<>();
        {
            var sys = new HashMap<String, Object>();
            sys.put("role", "system");
            sys.put("content", system);
            messages.add(sys);
        }
        {
            var usr = new HashMap<String, Object>();
            usr.put("role", "user");
            usr.put("content", userMsg);
            messages.add(usr);
        }

        var req = new HashMap<String, Object>();
        req.put("model", "gpt-4o-mini");
        req.put("temperature", 0.2);
        req.put("max_tokens", 400);
        req.put("messages", messages);

        var entity = new HttpEntity<>(req, headers);
        var resp = rest.exchange(
                "https://api.openai.com/v1/chat/completions",
                HttpMethod.POST,
                entity,
                ChatCompletionResp.class
        );

        String reply = "";
        if (resp.getBody() != null &&
                resp.getBody().choices != null &&
                !resp.getBody().choices.isEmpty() &&
                resp.getBody().choices.get(0).message != null) {
            reply = String.valueOf(resp.getBody().choices.get(0).message.content);
        }
        return java.util.Map.of("reply", reply == null ? "" : reply.trim());
    }

    // ===== helpers =====
    private String toJson(Object obj) {
        if (obj instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(toJson(list.get(i)));
            }
            return sb.append(']').toString();
        }
        if (obj instanceof Map<?,?> map) {
            StringBuilder sb = new StringBuilder("{");
            int i = 0;
            for (var e : map.entrySet()) {
                if (i++ > 0) sb.append(',');
                sb.append('"').append(escape(String.valueOf(e.getKey()))).append('"').append(':');
                sb.append(toJson(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (obj instanceof String s) return '"' + escape(s) + '"';
        if (obj instanceof Number || obj instanceof Boolean) return String.valueOf(obj);
        return "null";
    }

    private String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
    private String formatVnd(Number n) {
        if (n == null) return "";
        return String.format("%,d đ", n.longValue());
    }

    // minimal response model
    public static class ChatCompletionResp {
        public List<Choice> choices;
        public static class Choice { public Message message; }
        public static class Message { public String role; public String content; }
    }
}
