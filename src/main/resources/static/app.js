const { useEffect, useMemo, useState } = React;

const TOKEN_KEY = "campaignhub_access_token";
const REFRESH_TOKEN_KEY = "campaignhub_refresh_token";
const MIN_AMOUNT = 0.01;

const emptyForm = {
  id: null,
  productId: "",
  name: "",
  keywords: [],
  keywordInput: "",
  bidAmount: "",
  campaignFund: "",
  status: "ON",
  town: "",
  radiusInKm: ""
};

function App() {
  const [token, setToken] = useState(localStorage.getItem(TOKEN_KEY) || "");
  const [refreshToken, setRefreshToken] = useState(localStorage.getItem(REFRESH_TOKEN_KEY) || "");
  const [authMode, setAuthMode] = useState("login");
  const [authForm, setAuthForm] = useState({ email: "", password: "", displayName: "" });
  const [products, setProducts] = useState([]);
  const [newProduct, setNewProduct] = useState({ name: "", description: "" });
  const [campaigns, setCampaigns] = useState([]);
  const [towns, setTowns] = useState([]);
  const [keywordSuggestions, setKeywordSuggestions] = useState([]);
  const [emeraldBalance, setEmeraldBalance] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const hasToken = Boolean(token);

  useEffect(() => {
    fetchTowns();
  }, []);

  useEffect(() => {
    if (hasToken) {
      loadSecuredData();
    }
  }, [hasToken]);

  useEffect(() => {
    const query = form.keywordInput.trim();
    const timer = setTimeout(() => {
      fetchKeywords(query);
    }, 220);
    return () => clearTimeout(timer);
  }, [form.keywordInput]);

  const canSubmitCampaign = useMemo(() => {
    return (
      form.productId &&
      form.name.trim() &&
      form.keywords.length > 0 &&
      Number(form.bidAmount) >= MIN_AMOUNT &&
      Number(form.campaignFund) >= MIN_AMOUNT &&
      form.status &&
      form.town &&
      Number(form.radiusInKm) > 0
    );
  }, [form]);

  async function api(path, options = {}, requiresAuth = true) {
    const headers = {
      "Content-Type": "application/json",
      ...(options.headers || {})
    };

    if (requiresAuth && token) {
      headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(path, { ...options, headers });
    if (response.status === 204) {
      return null;
    }

    const contentType = response.headers.get("content-type") || "";
    const isJson = contentType.includes("json");
    const rawBody = await response.text();
    const payload = parseResponseBody(rawBody, isJson);

    if (!response.ok) {
      if (response.status === 401 && requiresAuth) {
        logout();
      }
      throw new Error(parseApiError(payload, rawBody) || `Request failed (${response.status})`);
    }

    return payload;
  }

  function parseResponseBody(rawBody, isJson) {
    if (!rawBody) {
      return null;
    }
    if (!isJson) {
      return rawBody;
    }
    try {
      return JSON.parse(rawBody);
    } catch {
      return rawBody;
    }
  }

  function parseApiError(payload, rawBody) {
    if (typeof payload === "string" && payload.trim()) {
      return payload;
    }
    if (!payload) {
      if (rawBody && rawBody.trim()) {
        return rawBody;
      }
      return "Unknown API error";
    }
    if (Array.isArray(payload.errors) && payload.errors.length > 0) {
      return payload.errors.join(" | ");
    }
    return payload.detail || payload.message || payload.title || "Unknown API error";
  }

  async function fetchTowns() {
    try {
      const data = await api("/api/towns", {}, false);
      setTowns(data || []);
    } catch {
      setTowns([]);
    }
  }

  async function fetchKeywords(query) {
    try {
      const qs = query ? `?query=${encodeURIComponent(query)}` : "";
      const data = await api(`/api/keywords${qs}`, {}, false);
      setKeywordSuggestions(data || []);
    } catch {
      setKeywordSuggestions([]);
    }
  }

  async function loadSecuredData() {
    setLoading(true);
    setError("");
    try {
      const [productsData, campaignsData, balanceData] = await Promise.all([
        api("/api/products"),
        api("/api/campaigns"),
        api("/api/emerald-account/balance")
      ]);
      setProducts(productsData || []);
      setCampaigns(campaignsData || []);
      setEmeraldBalance(balanceData?.balance ?? null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function saveTokenPair(tokenResponse) {
    localStorage.setItem(TOKEN_KEY, tokenResponse.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, tokenResponse.refreshToken);
    setToken(tokenResponse.accessToken);
    setRefreshToken(tokenResponse.refreshToken);
  }

  async function handleAuthSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    try {
      const payload =
        authMode === "register"
          ? {
              email: authForm.email,
              displayName: authForm.displayName,
              password: authForm.password
            }
          : {
              email: authForm.email,
              password: authForm.password
            };

      const endpoint = authMode === "register" ? "/api/auth/register" : "/api/auth/login";
      const tokenResponse = await api(endpoint, { method: "POST", body: JSON.stringify(payload) }, false);
      saveTokenPair(tokenResponse);
      setSuccess(authMode === "register" ? "Account created." : "Logged in.");
      setAuthForm({ email: "", password: "", displayName: "" });
    } catch (err) {
      setError(err.message);
    }
  }

  async function logout() {
    if (refreshToken) {
      try {
        await api(
          "/api/auth/logout",
          { method: "POST", body: JSON.stringify({ refreshToken }) },
          false
        );
      } catch {
        // no-op
      }
    }
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    setToken("");
    setRefreshToken("");
    setProducts([]);
    setCampaigns([]);
    setEmeraldBalance(null);
    setForm(emptyForm);
    setSuccess("Logged out.");
  }

  async function addProduct(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    try {
      const created = await api("/api/products", {
        method: "POST",
        body: JSON.stringify(newProduct)
      });
      setProducts((prev) => [...prev, created]);
      setNewProduct({ name: "", description: "" });
      setSuccess("Product added.");
    } catch (err) {
      setError(err.message);
    }
  }

  async function deleteProduct(productId) {
    setError("");
    setSuccess("");
    try {
      await api(`/api/products/${productId}`, { method: "DELETE" });
      setProducts((prev) => prev.filter((p) => p.id !== productId));
      setCampaigns((prev) => prev.filter((c) => c.productId !== productId));
      if (form.productId === productId) {
        setForm(emptyForm);
      }
      setSuccess("Product deleted.");
    } catch (err) {
      setError(err.message);
    }
  }

  function addKeyword(value) {
    const cleaned = value.trim();
    if (!cleaned) return;
    if (form.keywords.some((k) => k.toLowerCase() === cleaned.toLowerCase())) return;
    setForm((prev) => ({ ...prev, keywords: [...prev.keywords, cleaned], keywordInput: "" }));
  }

  function removeKeyword(keyword) {
    setForm((prev) => ({
      ...prev,
      keywords: prev.keywords.filter((k) => k !== keyword)
    }));
  }

  function editCampaign(campaign) {
    setForm({
      id: campaign.id,
      productId: campaign.productId,
      name: campaign.name,
      keywords: [...campaign.keywords],
      keywordInput: "",
      bidAmount: campaign.bidAmount,
      campaignFund: campaign.campaignFund,
      status: campaign.status,
      town: campaign.town || "",
      radiusInKm: campaign.radiusInKm
    });
    setSuccess("Editing campaign.");
    setError("");
  }

  function resetCampaignForm() {
    setForm(emptyForm);
    setError("");
  }

  async function submitCampaign(e) {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (!canSubmitCampaign) {
      setError("Fill all mandatory campaign fields.");
      return;
    }

    const payload = {
      productId: form.productId,
      name: form.name.trim(),
      keywords: form.keywords,
      bidAmount: Number(form.bidAmount),
      campaignFund: Number(form.campaignFund),
      status: form.status,
      town: form.town,
      radiusInKm: Number(form.radiusInKm)
    };

    try {
      const isEdit = Boolean(form.id);
      const result = await api(isEdit ? `/api/campaigns/${form.id}` : "/api/campaigns", {
        method: isEdit ? "PUT" : "POST",
        body: JSON.stringify(payload)
      });

      const updatedCampaign = result.campaign;
      setEmeraldBalance(result.emeraldBalance);
      if (isEdit) {
        setCampaigns((prev) => prev.map((c) => (c.id === updatedCampaign.id ? updatedCampaign : c)));
      } else {
        setCampaigns((prev) => [updatedCampaign, ...prev]);
      }

      setForm(emptyForm);
      setSuccess(isEdit ? "Campaign updated." : "Campaign created.");
    } catch (err) {
      setError(err.message);
    }
  }

  async function deleteCampaign(campaignId) {
    setError("");
    setSuccess("");
    try {
      await api(`/api/campaigns/${campaignId}`, { method: "DELETE" });
      setCampaigns((prev) => prev.filter((c) => c.id !== campaignId));
      const balanceData = await api("/api/emerald-account/balance");
      setEmeraldBalance(balanceData?.balance ?? null);
      if (form.id === campaignId) {
        setForm(emptyForm);
      }
      setSuccess("Campaign deleted.");
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="app-shell">
      <main className="layout">
        <section className="card">
          <h1>CampaignHub</h1>
          <p className="muted">Manage campaigns per product with live Emerald balance updates.</p>
          {error ? <div className="notice error">{error}</div> : null}
          {success ? <div className="notice success">{success}</div> : null}

          {!hasToken ? (
            <form className="stack" onSubmit={handleAuthSubmit}>
              <div className="mode-switch">
                <button type="button" onClick={() => setAuthMode("login")} className={authMode === "login" ? "active" : ""}>Login</button>
                <button type="button" onClick={() => setAuthMode("register")} className={authMode === "register" ? "active" : ""}>Register</button>
              </div>
              <input
                required
                type="email"
                placeholder="Email"
                value={authForm.email}
                onChange={(e) => setAuthForm((prev) => ({ ...prev, email: e.target.value }))}
              />
              {authMode === "register" ? (
                <input
                  required
                  type="text"
                  placeholder="Display name"
                  value={authForm.displayName}
                  onChange={(e) => setAuthForm((prev) => ({ ...prev, displayName: e.target.value }))}
                />
              ) : null}
              <input
                required
                type="password"
                placeholder="Password"
                value={authForm.password}
                onChange={(e) => setAuthForm((prev) => ({ ...prev, password: e.target.value }))}
              />
              <button type="submit">{authMode === "register" ? "Create account" : "Sign in"}</button>
            </form>
          ) : (
            <div className="stack">
              <div className="topbar">
                <span>
                  Emerald balance: <strong>{emeraldBalance !== null ? Number(emeraldBalance).toFixed(2) : "--"}</strong>
                </span>
                <button type="button" className="secondary" onClick={logout}>Logout</button>
              </div>

              <form className="stack subcard" onSubmit={addProduct}>
                <h2>Products</h2>
                <input
                  required
                  placeholder="Product name"
                  value={newProduct.name}
                  onChange={(e) => setNewProduct((prev) => ({ ...prev, name: e.target.value }))}
                />
                <input
                  placeholder="Product description"
                  value={newProduct.description}
                  onChange={(e) => setNewProduct((prev) => ({ ...prev, description: e.target.value }))}
                />
                <button type="submit">Add product</button>
                <div className="table-list">
                  {products.map((p) => (
                    <div key={p.id} className="row">
                      <span>{p.name}</span>
                      <button type="button" className="danger" onClick={() => deleteProduct(p.id)}>Delete</button>
                    </div>
                  ))}
                  {products.length === 0 ? <p className="muted">Add a product to create its campaign.</p> : null}
                </div>
              </form>

              <form className="stack subcard" onSubmit={submitCampaign}>
                <h2>{form.id ? "Edit campaign" : "New campaign"}</h2>

                <select
                  required
                  value={form.productId}
                  onChange={(e) => setForm((prev) => ({ ...prev, productId: e.target.value }))}
                >
                  <option value="">Select product</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>{p.name}</option>
                  ))}
                </select>

                <input
                  required
                  placeholder="Campaign name"
                  value={form.name}
                  onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
                />

                <div className="keyword-box">
                  <input
                    required={form.keywords.length === 0}
                    placeholder="Keywords"
                    value={form.keywordInput}
                    onChange={(e) => setForm((prev) => ({ ...prev, keywordInput: e.target.value }))}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault();
                        addKeyword(form.keywordInput || keywordSuggestions[0] || "");
                      }
                    }}
                  />
                  <button
                    type="button"
                    className="secondary"
                    onClick={() => addKeyword(form.keywordInput || keywordSuggestions[0] || "")}
                  >
                    Add
                  </button>
                </div>

                {keywordSuggestions.length > 0 ? (
                  <div className="suggestions">
                    {keywordSuggestions.slice(0, 8).map((keyword) => (
                      <button key={keyword} type="button" onClick={() => addKeyword(keyword)}>
                        {keyword}
                      </button>
                    ))}
                  </div>
                ) : null}

                {form.keywords.length > 0 ? (
                  <div className="chips">
                    {form.keywords.map((keyword) => (
                      <button type="button" key={keyword} className="chip" onClick={() => removeKeyword(keyword)}>
                        {keyword} x
                      </button>
                    ))}
                  </div>
                ) : null}

                <div className="grid-2">
                  <input
                    required
                    type="number"
                    min={MIN_AMOUNT}
                    step="0.01"
                    placeholder="Bid amount"
                    value={form.bidAmount}
                    onChange={(e) => setForm((prev) => ({ ...prev, bidAmount: e.target.value }))}
                  />
                  <input
                    required
                    type="number"
                    min={MIN_AMOUNT}
                    step="0.01"
                    placeholder="Campaign fund"
                    value={form.campaignFund}
                    onChange={(e) => setForm((prev) => ({ ...prev, campaignFund: e.target.value }))}
                  />
                </div>

                <div className="grid-2">
                  <select
                    required
                    value={form.status}
                    onChange={(e) => setForm((prev) => ({ ...prev, status: e.target.value }))}
                  >
                    <option value="ON">ON</option>
                    <option value="OFF">OFF</option>
                  </select>

                  <select
                    required
                    value={form.town}
                    onChange={(e) => setForm((prev) => ({ ...prev, town: e.target.value }))}
                  >
                    <option value="">Select town</option>
                    {towns.map((town) => (
                      <option key={town} value={town}>{town}</option>
                    ))}
                  </select>
                </div>

                <input
                  required
                  type="number"
                  min="1"
                  step="1"
                  placeholder="Radius in km"
                  value={form.radiusInKm}
                  onChange={(e) => setForm((prev) => ({ ...prev, radiusInKm: e.target.value }))}
                />

                <div className="actions">
                  <button type="submit" disabled={!canSubmitCampaign}>
                    {form.id ? "Save changes" : "Create campaign"}
                  </button>
                  {form.id ? (
                    <button type="button" className="secondary" onClick={resetCampaignForm}>Cancel edit</button>
                  ) : null}
                </div>
              </form>

              <section className="stack subcard">
                <h2>Campaigns</h2>
                {loading ? <p className="muted">Loading...</p> : null}
                {campaigns.length === 0 && !loading ? <p className="muted">No campaigns yet.</p> : null}
                {campaigns.map((campaign) => (
                  <article key={campaign.id} className="campaign-row">
                    <div>
                      <strong>{campaign.name}</strong>
                      <p className="muted">
                        {products.find((p) => p.id === campaign.productId)?.name || "Unknown product"} | {campaign.status} | {campaign.town} | {campaign.radiusInKm} km
                      </p>
                      <p className="muted">Keywords: {[...campaign.keywords].join(", ")}</p>
                    </div>
                    <div className="actions">
                      <button type="button" className="secondary" onClick={() => editCampaign(campaign)}>Edit</button>
                      <button type="button" className="danger" onClick={() => deleteCampaign(campaign.id)}>Delete</button>
                    </div>
                  </article>
                ))}
              </section>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<App />);
