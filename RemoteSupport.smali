.class public final Ltv/RemoteSupport;
.super Ljava/lang/Object;
.implements Landroid/view/ViewTreeObserver$OnGlobalLayoutListener;

.field private root:Landroid/view/View;

.method private constructor <init>(Landroid/view/View;)V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-object p1, p0, Ltv/RemoteSupport;->root:Landroid/view/View;
    return-void
.end method

.method public static install(Landroid/app/Activity;)V
    .locals 4
    invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
    move-result-object v0
    invoke-virtual {v0}, Landroid/view/Window;->getDecorView()Landroid/view/View;
    move-result-object v0
    const v1, 0x7f0afffe
    invoke-virtual {v0, v1}, Landroid/view/View;->getTag(I)Ljava/lang/Object;
    move-result-object v2
    if-nez v2, :done
    new-instance v2, Ltv/RemoteSupport;
    invoke-direct {v2, v0}, Ltv/RemoteSupport;-><init>(Landroid/view/View;)V
    invoke-virtual {v0, v1, v2}, Landroid/view/View;->setTag(ILjava/lang/Object;)V
    invoke-virtual {v0}, Landroid/view/View;->getViewTreeObserver()Landroid/view/ViewTreeObserver;
    move-result-object v3
    invoke-virtual {v3, v2}, Landroid/view/ViewTreeObserver;->addOnGlobalLayoutListener(Landroid/view/ViewTreeObserver$OnGlobalLayoutListener;)V
    invoke-static {v0}, Ltv/RemoteSupport;->decorate(Landroid/view/View;)V
    :done
    return-void
.end method

.method private static decorate(Landroid/view/View;)V
    .locals 4
    invoke-virtual {p0}, Landroid/view/View;->isClickable()Z
    move-result v0
    if-eqz v0, :children
    const/4 v0, 0x1
    invoke-virtual {p0, v0}, Landroid/view/View;->setFocusable(Z)V
    invoke-virtual {p0, v0}, Landroid/view/View;->setDefaultFocusHighlightEnabled(Z)V
    const v1, 0x7f0afffd
    invoke-virtual {p0, v1}, Landroid/view/View;->getTag(I)Ljava/lang/Object;
    move-result-object v2
    if-nez v2, :children
    invoke-virtual {p0}, Landroid/view/View;->getContext()Landroid/content/Context;
    move-result-object v2
    invoke-virtual {v2}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
    move-result-object v3
    invoke-virtual {v2}, Landroid/content/Context;->getPackageName()Ljava/lang/String;
    move-result-object v2
    const-string v0, "tv_focus"
    const-string v1, "drawable"
    invoke-virtual {v3, v0, v1, v2}, Landroid/content/res/Resources;->getIdentifier(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
    move-result v0
    if-eqz v0, :children
    invoke-virtual {v3, v0}, Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;
    move-result-object v0
    invoke-virtual {p0, v0}, Landroid/view/View;->setForeground(Landroid/graphics/drawable/Drawable;)V
    const v1, 0x7f0afffd
    sget-object v2, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
    invoke-virtual {p0, v1, v2}, Landroid/view/View;->setTag(ILjava/lang/Object;)V
    :children
    instance-of v0, p0, Landroid/view/ViewGroup;
    if-eqz v0, :done
    check-cast p0, Landroid/view/ViewGroup;
    invoke-virtual {p0}, Landroid/view/ViewGroup;->getChildCount()I
    move-result v0
    const/4 v1, 0x0
    :loop
    if-ge v1, v0, :done
    invoke-virtual {p0, v1}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;
    move-result-object v2
    invoke-static {v2}, Ltv/RemoteSupport;->decorate(Landroid/view/View;)V
    add-int/lit8 v1, v1, 0x1
    goto :loop
    :done
    return-void
.end method

.method public onGlobalLayout()V
    .locals 1
    iget-object v0, p0, Ltv/RemoteSupport;->root:Landroid/view/View;
    invoke-static {v0}, Ltv/RemoteSupport;->decorate(Landroid/view/View;)V
    return-void
.end method

.method public static activate(Landroid/app/Activity;Landroid/view/KeyEvent;)Z
    .locals 3
    invoke-virtual {p1}, Landroid/view/KeyEvent;->getKeyCode()I
    move-result v0
    const/16 v1, 0x17
    if-eq v0, v1, :target
    const/16 v1, 0x42
    if-eq v0, v1, :target
    const/16 v1, 0xa0
    if-ne v0, v1, :no
    :target
    invoke-virtual {p0}, Landroid/app/Activity;->getCurrentFocus()Landroid/view/View;
    move-result-object v0
    if-eqz v0, :no
    instance-of v1, v0, Landroid/widget/EditText;
    if-nez v1, :no
    invoke-virtual {v0}, Landroid/view/View;->isEnabled()Z
    move-result v1
    if-eqz v1, :no
    invoke-virtual {v0}, Landroid/view/View;->isClickable()Z
    move-result v1
    if-eqz v1, :no
    invoke-virtual {p1}, Landroid/view/KeyEvent;->getAction()I
    move-result v1
    const/4 v2, 0x1
    if-ne v1, v2, :down
    invoke-virtual {p1}, Landroid/view/KeyEvent;->isCanceled()Z
    move-result v1
    if-nez v1, :handled
    invoke-virtual {v0}, Landroid/view/View;->performClick()Z
    goto :handled
    :down
    if-nez v1, :no
    :handled
    const/4 v0, 0x1
    return v0
    :no
    const/4 v0, 0x0
    return v0
.end method
