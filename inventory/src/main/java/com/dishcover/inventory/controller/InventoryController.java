package com.dishcover.inventory.controller;

import com.dishcover.common.security.AuthenticatedUser;
import com.dishcover.inventory.dto.InventoryDtos.AddItemRequest;
import com.dishcover.inventory.dto.InventoryDtos.BatchAddRequest;
import com.dishcover.inventory.dto.InventoryDtos.InventoryItemResponse;
import com.dishcover.inventory.dto.InventoryDtos.UpdateItemRequest;
import com.dishcover.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** userId luôn lấy từ JWT (specs/inventory-service.md mục 3) — không bao giờ nhận từ client. */
@RestController
@RequestMapping("/inventory/items")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<InventoryItemResponse> list(@AuthenticationPrincipal AuthenticatedUser me,
                                            @RequestParam(required = false) String status) {
        return service.list(me.userId(), status);
    }

    @GetMapping("/{id}")
    public InventoryItemResponse getOne(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        return service.getOne(me.userId(), id);
    }

    @PostMapping
    public ResponseEntity<InventoryItemResponse> add(@AuthenticationPrincipal AuthenticatedUser me,
                                                      @Valid @RequestBody AddItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addItem(me.userId(), req));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<InventoryItemResponse>> addBatch(@AuthenticationPrincipal AuthenticatedUser me,
                                                                 @Valid @RequestBody BatchAddRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addBatch(me.userId(), req.items()));
    }

    @PatchMapping("/{id}")
    public InventoryItemResponse update(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                                        @Valid @RequestBody UpdateItemRequest req) {
        return service.update(me.userId(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        service.delete(me.userId(), id);
    }
}
