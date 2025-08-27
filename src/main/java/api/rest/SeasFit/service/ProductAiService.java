package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.AiSuggestRequest;
import api.rest.SeasFit.dto.AiSuggestResponse;
import api.rest.SeasFit.dto.ProductListDTO;
import com.openai.client.OpenAIClient;
import com.openai.models.*;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputText;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductAiService {

  private final OpenAIClient openai;
  private final ProductService productService;

//  public AiSuggestResponse recommend(AiSuggestRequest req) {
//    int limit = Math.max(1, Math.min(12, req.limit() == null ? 8 : req.limit()));
//
//    Page<ProductListDTO> page = productService.findAllWithFilters(
//            req.categoryId(), null, null, req.minPrice(), req.maxPrice(),
//            0, limit, Sort.unsorted()
//    );
//    List<ProductListDTO> items = page.getContent();
//
//    StringBuilder ctx = new StringBuilder();
//    for (ProductListDTO p : items) {
//      String priceLine = toPriceLine(p.getPrice(), p.getFinalPrice(), p.isOnSale());
//      int colorCount = p.getColors() == null ? 0 : p.getColors().size();
//      int sizeCount = p.getSizes() == null ? 0 : p.getSizes().size();
//
//      ctx.append("- ")
//              .append(p.getName())
//              .append(" | ").append(priceLine)
//              .append(" | màu: ").append(colorCount)
//              .append(" | size: ").append(sizeCount)
//              .append("\n");
//    }
//
//    String userMsg = req.message() == null ? "" : req.message();
//    String prompt = String.format("""
//                Bạn là trợ lý bán hàng của 4S SeasFit. Dữ liệu mỗi sản phẩm gồm: tên, giá gốc, giá sau sale (nếu có), số màu, số size.
//                Hãy tư vấn NGẮN GỌN (<=120 từ), bullet point rõ ràng, gợi ý 3–5 lựa chọn phù hợp với yêu cầu của user.
//                Cuối cùng chốt bằng 1 CTA ngắn kiểu: "Xem chi tiết mẫu X nhé!".
//
//                Danh sách sản phẩm:
//                %s
//
//                Yêu cầu của user: "%s"
//                """, ctx, userMsg);
//
//    String answer;
//    try {
//      ResponseCreateParams params = ResponseCreateParams.builder()
//              .model(ChatModel.GPT_4_1_MINI) // hoặc ChatModel.GPT_4_1
//              .input(prompt)
//              .build();
//
//      Response resp = openai.responses().create(params);
//      answer = extractText(resp);
//      if (answer == null || answer.isBlank()) {
//        answer = "Dưới đây là vài sản phẩm hợp lý nhé:";
//      }
//    } catch (Exception e) {
//      answer = "Dưới đây là vài sản phẩm nổi bật phù hợp tiêu chí của bạn:";
//    }
//
//    return new AiSuggestResponse(answer, items);
//  }

  // ===== Helpers =====
  private static String toPriceLine(BigDecimal price, BigDecimal finalPrice, boolean onSale) {
    BigDecimal goc = price;
    BigDecimal cuoi = finalPrice;
    if (cuoi != null && (onSale || (goc != null && cuoi.compareTo(goc) < 0))) {
      return fmtVnd(cuoi) + " (sale từ " + fmtVnd(goc) + ")";
    }
    return fmtVnd(goc != null ? goc : cuoi);
  }

  private static String fmtVnd(BigDecimal v) {
    if (v == null) return "?₫";
    return NumberFormat.getInstance(new Locale("vi", "VN")).format(v) + "₫";
  }

//  private static String extractText(Response resp) {
//    if (resp == null || resp.output() == null) return "";
//    return resp.output().stream()
//            .flatMap((ResponseOutput o) -> o.content().stream())
//            .filter(c -> c instanceof ResponseOutputText)
//            .map(c -> ((ResponseOutputText) c).text().value())
//            .collect(Collectors.joining());
//  }
}
