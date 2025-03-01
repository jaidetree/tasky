defmodule TaksyWeb.ManifestHelper do
  @moduledoc """
  Helper functions for working with the Vite manifest.json file
  """

  # Import for the `raw` function
  import Phoenix.HTML

  @doc """
  Returns the path to the specified entrypoint from the Vite manifest.
  """
  def entrypoint_from_manifest(manifest_path, entrypoint) do
    manifest_path
    |> read_manifest()
    |> get_entrypoint_path(entrypoint)
    |> prefix_asset_path()
  end

  @doc """
  Renders all CSS files associated with an entrypoint, including imports.
  Returns raw HTML for all CSS link tags.
  """
  def render_css_from_manifest(manifest_path, entrypoint) do
    manifest = read_manifest(manifest_path)

    case manifest[entrypoint] do
      %{"css" => css_files} when is_list(css_files) ->
        css_files
        |> Enum.map(fn css_file ->
          raw("<link rel=\"stylesheet\" href=\"/assets/#{css_file}\">")
        end)

      _ ->
        []
    end
  end

  @doc """
  Renders the main script tag and all its imported chunks.
  Returns raw HTML for all script tags.
  """
  def render_js_from_manifest(manifest_path, entrypoint) do
    manifest = read_manifest(manifest_path)

    case manifest[entrypoint] do
      %{"file" => file, "imports" => imports} ->
        # First, render all the imported chunks (they need to load first)
        import_tags =
          for import_file <- imports do
            case manifest[import_file] do
              %{"file" => chunk_file} ->
                raw("<script type=\"module\" defer src=\"/assets/#{chunk_file}\"></script>")

              _ ->
                ""
            end
          end

        # Then render the main file
        main_tag =
          raw("<script type=\"module\" defer src=\"/assets/#{file}\"></script>")

        import_tags ++ [main_tag]

      %{"file" => file} ->
        [raw("<script type=\"module\" defer src=\"/assets/#{file}\"></script>")]

      _ ->
        []
    end
  end

  defp read_manifest(manifest_path) do
    manifest_path
    |> File.read!()
    |> Jason.decode!()
  rescue
    _ -> %{}
  end

  defp get_entrypoint_path(manifest, entrypoint) do
    case manifest[entrypoint] do
      %{"file" => file} -> file
      _ -> entrypoint
    end
  end

  defp prefix_asset_path(path) do
    "/assets/#{path}"
  end
end
