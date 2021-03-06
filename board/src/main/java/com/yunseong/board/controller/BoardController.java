package com.yunseong.board.controller;

import com.yunseong.board.domain.Board;
import com.yunseong.board.domain.BoardRevision;
import com.yunseong.board.domain.Comment;
import com.yunseong.board.service.BoardService;
import com.yunseong.board.service.FileService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(value = "/boards")
@AllArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final FileService fileService;

    @GetMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PostAuthorize("(isAnonymous() and (returnObject.body.category.readPermission.name() == 'ANONYMOUS')) or (#oauth2.hasScope('board_read') and hasRole('ROLE_' + returnObject.body.category.readPermission.name()))")
    public ResponseEntity<BoardDetailResponse> findBoard(@PathVariable long id) {
        return ResponseEntity.ok(this.boardService.findBoard(id));
    }

    @GetMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedModel<BoardSimpleResponse>> findMyBoards(Principal principal, @PageableDefault Pageable pageable) {
        Page<BoardSimpleResponse> page = this.boardService.findMyBoards(principal.getName(), pageable).map(b -> new BoardSimpleResponse(b.getId(), b.getSubject(), b.getContent(), b.getBoardCategory(), b.getCreatedTime()));
        PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages());
        PagedModel<BoardSimpleResponse> model = PagedModel.of(page.getContent(), metadata);
        return ResponseEntity.ok(model);
    }

    @GetMapping(value = "/best", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<HotBoardResponse>> findHotBoards(@ModelAttribute HotBoardSearchCondition boardSearchCondition) {
        return ResponseEntity.ok(this.boardService.findsHotBoard(boardSearchCondition));
    }

    @GetMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedModel<BoardSearchResponse>> findBoards(@ModelAttribute BoardSearchCondition boardSearchCondition, @PageableDefault Pageable pageable) {
        Page<BoardSearchResponse> page = this.boardService.findsBoard(boardSearchCondition, pageable);
        PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages());
        PagedModel<BoardSearchResponse> model = PagedModel.of(page.getContent(), metadata);
        return ResponseEntity.ok(model);
    }

    @GetMapping(value = "{id}/comments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedModel<CommentResponse>> findCommentsByBoard(@PathVariable long id, @PageableDefault Pageable pageable) {
        Page<CommentResponse> page = this.boardService.findCommentByPage(id, pageable).map(c -> new CommentResponse(c.getId(), c.getWriter(), c.getContent(), c.getCreatedTime(), c.getCommentState()));
        PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages());
        PagedModel<CommentResponse> model = PagedModel.of(page.getContent(), metadata);
        return ResponseEntity.ok(model);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("#oauth2.hasScope('board_write') and hasRole('ROLE_' + #request.category.writePermission.name())")
    public ResponseEntity<Long> createBoard(@ModelAttribute BoardCreateRequest request, @RequestPart(required = false, name = "file") MultipartFile[] files, Principal principal) {
        Board board = this.boardService.createBoard(principal.getName(), request, false);
        this.fileService.save(board.getId(), files);
        return new ResponseEntity<>(board.getId(), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ROLE_' + @boardService.getCategory(#id).writePermission.name())")
    @PostMapping(value = "/{id}/comments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> createComment(@PathVariable long id, @RequestBody CommentCreateRequest request, Principal principal) {
        Comment comment = this.boardService.createComment(id, principal.getName(), request);
        return new ResponseEntity<>(comment.getId(), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ROLE_' + @boardService.getCategory(#id).writePermission.name())")
    @PutMapping(value = "/comments/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> reviseComment(@PathVariable long id, @RequestBody CommentReviseRequest reviseRequest, Principal principal) {
        return ResponseEntity.ok(this.boardService.reviseComment(id, principal.getName(), reviseRequest.getContent()).getId());
    }

    @PreAuthorize("hasRole('ROLE_' + @boardService.getCategory(#id).writePermission.name())")
    @PutMapping(value = "/{id}/recommend", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> recommendBoard(@PathVariable long id, Principal principal) {
        return ResponseEntity.ok(this.boardService.recommendBoard(id, principal.getName()).getId());
    }

    @PreAuthorize("hasRole('ROLE_' + @boardService.getCategory(#id).writePermission.name())")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> reviseBoard(@PathVariable long id, @ModelAttribute BoardRevision boardRevision, @RequestPart(required = false, name = "file") MultipartFile[] files, Principal principal) {
        Long bid = this.boardService.reviseBoard(id, principal.getName(), boardRevision).getId();
        this.fileService.save(bid, files);
        return ResponseEntity.ok(bid);
    }

    @DeleteMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteBoard(@PathVariable long id, Principal principal) {
        this.boardService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/comments/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteComment(@PathVariable long id, Principal principal) {
        this.boardService.deleteComment(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
