<!-- LOVABLE:BEGIN -->
> [!IMPORTANT]
> This project is connected to [Lovable](https://lovable.dev). Avoid rewriting
> published git history — force pushing, or rebasing/amending/squashing commits
> that are already pushed — as it rewrites history on Lovable's side and the
> user will likely lose their project history.
>
> Commits you push to the connected branch sync back to Lovable and show up in
> the editor, so keep the branch in a working state.
<!-- LOVABLE:END -->

- Keep fictional marketplace records in `src/data/demo.ts`, separate from any future production data source, so sample data cannot be confused with real records.
- Share transient cart, wishlist, and recently viewed state through the MarketHub DemoStore provider; no persistence or authentication is implied.
- Use reusable MarketHub feature components with thin TanStack page routes, and keep role-specific workspace navigation under `/dashboard/$role` with a validated section search parameter.
- Define interface styling and semantic color tokens in `src/styles.css` so storefront and workspace surfaces stay consistent.
