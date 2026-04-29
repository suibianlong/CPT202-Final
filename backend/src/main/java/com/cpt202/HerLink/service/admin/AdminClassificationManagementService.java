package com.cpt202.HerLink.service.admin;

import com.cpt202.HerLink.dto.admin.AdminCategoryRequest;
import com.cpt202.HerLink.dto.admin.AdminCategoryResponse;
import com.cpt202.HerLink.dto.admin.AdminResourceTypeRequest;
import com.cpt202.HerLink.dto.admin.AdminResourceTypeResponse;
import com.cpt202.HerLink.dto.admin.AdminTagRequest;
import com.cpt202.HerLink.dto.admin.AdminTagResponse;
import java.util.List;

public interface AdminClassificationManagementService {

    List<AdminCategoryResponse> getAllCategories();

    List<AdminCategoryResponse> getActiveCategories();

    AdminCategoryResponse createCategory(AdminCategoryRequest request, String administrator);

    AdminCategoryResponse updateCategory(Long categoryId, AdminCategoryRequest request, String administrator);

    AdminCategoryResponse deactivateCategory(Long categoryId, String administrator);

    AdminCategoryResponse activateCategory(Long categoryId, String administrator);

    List<AdminTagResponse> getAllTags();

    List<AdminTagResponse> getActiveTags();

    AdminTagResponse createTag(AdminTagRequest request, String administrator);

    AdminTagResponse updateTag(Long tagId, AdminTagRequest request, String administrator);

    AdminTagResponse deactivateTag(Long tagId, String administrator);

    AdminTagResponse activateTag(Long tagId, String administrator);

    List<AdminResourceTypeResponse> getAllResourceTypes();

    List<AdminResourceTypeResponse> getActiveResourceTypes();

    AdminResourceTypeResponse createResourceType(AdminResourceTypeRequest request, String administrator);

    AdminResourceTypeResponse updateResourceType(Long resourceTypeId, AdminResourceTypeRequest request, String administrator);

    AdminResourceTypeResponse deactivateResourceType(Long resourceTypeId, String administrator);

    AdminResourceTypeResponse activateResourceType(Long resourceTypeId, String administrator);
}
