package api.rest.SeasFit.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

@Component
public class ChatResponseLoader {
    private final Map<String, List<String>> responses;
    private final Map<String, Queue<String>> smartQueues = new HashMap<>();

    public ChatResponseLoader() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getClassLoader().getResourceAsStream("chatbot-responses.json");
            responses = mapper.readValue(is, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Không thể load chatbot-responses.json", e);
        }
    }

    public String getReply(String keyword) {
        List<String> replyList = responses.getOrDefault(keyword, responses.get("mặc định"));

        smartQueues.putIfAbsent(keyword, shuffleQueue(replyList));

        Queue<String> queue = smartQueues.get(keyword);

        if (queue.isEmpty()) {
            queue = shuffleQueue(replyList);
            smartQueues.put(keyword, queue);
        }

        return queue.poll();
    }

    private Queue<String> shuffleQueue(List<String> list) {
        List<String> shuffled = new ArrayList<>(list);
        Collections.shuffle(shuffled);
        return new LinkedList<>(shuffled);
    }
}
