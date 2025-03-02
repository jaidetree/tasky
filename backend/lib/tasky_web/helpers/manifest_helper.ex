# /backend/lib/tasky_web/helpers/manifest_helper.ex
defmodule TaskyWeb.Helpers.ManifestHelper do
  @moduledoc """
  Helper functions for working with the Vite manifest.json file.
  This module is deliberately independent of other TaskyWeb modules
  to avoid circular dependencies.
  """

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
          # Use Phoenix.HTML.raw directly, not imported
          Phoenix.HTML.raw(
            "<link rel=\"stylesheet\" href=\"#{manifest_path |> Path.dirname()}/#{css_file}\">"
          )
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
                Phoenix.HTML.raw(
                  "<script type=\"module\" defer src=\"#{manifest_path |> Path.dirname()}/#{chunk_file}\"></script>"
                )

              _ ->
                ""
            end
          end

        # Then render the main file
        main_tag =
          Phoenix.HTML.raw(
            "<script type=\"module\" defer src=\"#{manifest_path |> Path.dirname()}/#{file}\"></script>"
          )

        import_tags ++ [main_tag]

      %{"file" => file} ->
        [
          Phoenix.HTML.raw(
            "<script type=\"module\" defer src=\"#{manifest_path |> Path.dirname()}/#{file}\"></script>"
          )
        ]

      _ ->
        []
    end
  end

  defp read_manifest(manifest_path) do
    priv_static_path = Path.join(:code.priv_dir(:tasky), "static#{manifest_path}")

    if File.exists?(priv_static_path) do
      priv_static_path
      |> File.read!()
      |> Jason.decode!()
    else
      %{}
    end
  rescue
    _ -> %{}
  end
end

