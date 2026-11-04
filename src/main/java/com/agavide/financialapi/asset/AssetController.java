package com.agavide.financialapi.asset;

import com.agavide.financialapi.asset.dto.AssetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Assets", description = "Search available instruments")
public class AssetController {

	private final AssetService assetService;

	public AssetController(AssetService assetService) {
		this.assetService = assetService;
	}

	@GetMapping
	@Operation(
			summary = "Search assets by ticker and/or name",
			description = "Returns a list of instruments matching the query. Search is case-insensitive and matches partial ticker or name. If no query is provided, all instruments are returned."
	)
	@ApiResponse(responseCode = "200", description = "List of matching assets",
			content = @Content(mediaType = "application/json",
					schema = @Schema(implementation = AssetResponse.class)))
	public ResponseEntity<List<AssetResponse>> searchAssets(
			@Parameter(description = "Search term for ticker or name", example = "TEST")
			@RequestParam(name = "query", required = false) String query) {
		return ResponseEntity.ok(assetService.searchAssets(query));
	}
}
