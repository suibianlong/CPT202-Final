package com.cpt202.HerLink.service.impl;

import com.cpt202.HerLink.entity.Tag;
import com.cpt202.HerLink.mapper.TagMapper;
import com.cpt202.HerLink.service.TagService;
import com.cpt202.HerLink.vo.CategoryTagOptionVO;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Provide active tag options for frontend selection.
@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    public TagServiceImpl(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    // Return active tags as selectable option objects.
    @Override
    public List<CategoryTagOptionVO> listTagOptions() {
        List<Tag> tagList = tagMapper.selectActiveTags();
        if (tagList == null) {
            tagList = Collections.emptyList();
        }

        List<CategoryTagOptionVO> optionVOList = new ArrayList<>();
        for (Tag tag : tagList) {
            CategoryTagOptionVO optionVO = new CategoryTagOptionVO();
            optionVO.setId(tag.getTagId());
            optionVO.setName(tag.getTagName());
            optionVOList.add(optionVO);
        }

        return optionVOList;
    }
}
