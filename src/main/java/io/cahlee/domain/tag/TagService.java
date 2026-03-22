package io.cahlee.domain.tag;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Transactional
    public List<Tag> findOrCreateTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }

        List<Tag> result = new ArrayList<>();
        for (String name : tagNames) {
            String trimmed = name.trim().toLowerCase();
            if (trimmed.isBlank()) continue;

            Tag tag = tagRepository.findByName(trimmed)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(trimmed).build()));
            result.add(tag);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Tag> findAll() {
        return tagRepository.findAll();
    }
}
