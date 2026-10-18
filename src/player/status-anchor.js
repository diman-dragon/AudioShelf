let anchor = { status: null, at: 0 };

export const setAnchor = (status) => {
  anchor = { status, at: performance.now() };
};

export const getAnchor = () => anchor;
